package ru.jamsys.pool;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.component.RateLimitManager;
import ru.jamsys.extension.RunnableInterface;
import ru.jamsys.rate.limit.RateLimit;
import ru.jamsys.rate.limit.item.RateLimitItem;
import ru.jamsys.rate.limit.RateLimitName;
import ru.jamsys.statistic.AbstractExpired;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@ToString(onlyExplicitlyIncluded = true)
public abstract class AbstractPool<T extends AbstractPoolItem<?>> extends AbstractExpired implements Pool<T>, RunnableInterface {

    public static ThreadLocal<Pool<?>> contextPool = new ThreadLocal<>();

    private final AtomicInteger max = new AtomicInteger(0); //Максимальное кол-во ресурсов

    private final int min; //Минимальное кол-во ресурсов

    private long sumTime = -1; //Сколько времени использовались ресурсы за 3сек

    @Getter
    @ToString.Include
    public final String name;

    @Setter
    private Function<Integer, Integer> formulaAddCount = (need) -> need;

    @Setter
    private Function<Integer, Integer> formulaRemoveCount = (need) -> need;

    protected final ConcurrentLinkedDeque<T> parkQueue = new ConcurrentLinkedDeque<>();

    protected final ConcurrentLinkedDeque<T> removeQueue = new ConcurrentLinkedDeque<>();

    protected final ConcurrentLinkedDeque<T> exceptionQueue = new ConcurrentLinkedDeque<>();

    protected final ConcurrentLinkedDeque<T> resourceQueue = new ConcurrentLinkedDeque<>();

    protected final AtomicBoolean isRun = new AtomicBoolean(false);

    private final AtomicBoolean restartOperation = new AtomicBoolean(false);

    private long timeWhenParkIsEmpty = -1;

    @Getter
    protected final RateLimit rateLimit;

    @Getter
    protected final RateLimit rateLimitPoolItem;

    public AbstractPool(String name, int min, int initMax, Class<T> cls) {
        this.name = name;
        this.max.set(initMax); // Может быть изменён в runTime
        this.min = min;
        rateLimit = App.context.getBean(RateLimitManager.class).get(getClass(), name);
        rateLimitPoolItem = App.context.getBean(RateLimitManager.class).get(cls, name);
    }

    @Override
    public void setSumTime(long time) {
        sumTime = time;
    }

    private long getTimeWhenParkIsEmpty() {
        if (timeWhenParkIsEmpty == -1) {
            return 0;
        } else {
            return System.currentTimeMillis() - timeWhenParkIsEmpty;
        }
    }

    private void checkPark() {
        if (parkQueue.isEmpty()) {
            if (timeWhenParkIsEmpty == -1) {
                timeWhenParkIsEmpty = System.currentTimeMillis();
            }
        } else {
            timeWhenParkIsEmpty = -1;
        }
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    public String getMomentumStatistic() {
        StringBuilder sb = new StringBuilder();
        sb.append("resourceQueue: ").append(resourceQueue.size()).append("; ");
        sb.append("parkQueue: ").append(parkQueue.size()).append("; ");
        sb.append("removeQueue: ").append(removeQueue.size()).append("; ");
        sb.append("isRun: ").append(isRun.get()).append("; ");
        sb.append("min: ").append(min).append("; ");
        sb.append("max: ").append(max.get()).append("; ");
        //sb.append("timePark0: ").append(getTimeWhenParkIsEmpty()).append("; ");
        return sb.toString();
    }

    public boolean isAmI() {
        return this.equals(AbstractPool.contextPool.get());
    }

    public boolean isEmpty() {
        return resourceQueue.isEmpty();
    }

    public void setMaxSlowRiseAndFastFall(int max) {
        RateLimitItem poolSize = rateLimit.get(RateLimitName.POOL_SIZE);
        if (poolSize.check(max)) {
            if (max >= min) {
                if (max > this.max.get()) { //Медленно поднимаем
                    this.max.incrementAndGet();
                } else { //Но очень быстро опускаем
                    this.max.set(max);
                }
            } else {
                Util.logConsole("Pool [" + getName() + "] sorry max = " + max + " < Pool.min = " + min, true);
            }
        } else {
            Util.logConsole("Pool [" + getName() + "] sorry max = " + max + " > RateLimit.max = " + poolSize.getMax(), true);
        }
    }

    @SuppressWarnings("unused")
    @Override
    public void complete(T resource, Exception e) {
        if (resource == null) {
            return;
        }
        if (checkExceptionOnComplete(e)) {
            exceptionQueue.add(resource);
            return;
        }
        if (resourceQueue.size() > max.get() || !isRun.get()) {
            if (addToRemoveWithoutCheckParking(resource)) {
                return;
            }
        }
        // Если взять потоки как ресурсы, то после createResource вызывается Thread.start, который после работы сам
        // себя вносит в пул отработанных (parkQueue)
        // Но вообще понятие ресурсов статично и они не живут собственной жизнью
        // поэтому после createResource мы кладём их в parkQueue, что бы их могли взять желающие
        // И для потоков тут получается первичный дубль
        if (!parkQueue.contains(resource)) {
            parkQueue.addLast(resource);
            checkPark();
        }
    }

    @SuppressWarnings({"unused"})
    @Override
    public T getResource() {
        if (!isRun.get()) {
            return null;
        }
        // Забираем с начала, что бы под нож улетели последние добавленные
        T resource = parkQueue.pollFirst();
        checkPark();
        if (resource != null) {
            resource.polled();
        }
        return resource;
    }

    @SuppressWarnings({"unused"})
    @Override
    public T getResource(Long timeOutMs) {
        if (!isRun.get()) {
            return null;
        }
        long finishTimeMs = System.currentTimeMillis() + timeOutMs;
        while (isRun.get() && finishTimeMs > System.currentTimeMillis()) {
            T resource = parkQueue.pollFirst();
            checkPark();
            if (resource != null) {
                resource.polled();
                return resource;
            }
            Util.sleepMs(100);
        }
        return null;
    }

    @SafeVarargs
    static <T> T[] getEmptyType(T... array) {
        return Arrays.copyOf(array, 0);
    }

    private boolean add() {
        if (isRun.get() && resourceQueue.size() < max.get()) {
            if (!removeQueue.isEmpty()) {
                T resource = removeQueue.pollLast();
                if (resource != null) {
                    parkQueue.add(resource);
                    checkPark();
                    return true;
                }
            }
            final T resource = createResource();
            if (resource != null) {
                //#1
                resourceQueue.add(resource);
                //#2
                parkQueue.add(resource);
                checkPark();
                return true;
            }
        }
        return false;
    }

    //Это когда поток придушили сторонними силами без участия пула
    public void removeForce(T resource) {
        resourceQueue.remove(resource);
        parkQueue.remove(resource);
        checkPark();
        removeQueue.remove(resource);
    }

    synchronized private void remove(@NonNull T resource) {
        if (resourceQueue.contains(resource)) {
            removeForce(resource);
            closeResource(resource); //Если выкидываем из пула, то наверное надо закрыть сам ресурс
        } else {
            App.context.getBean(ExceptionHandler.class).handler(new RuntimeException(
                    "Видимо ресурс был закрыт через removeForce, наши полномочия всё"
            ));
        }
    }

    private void overclocking(int count) {
        if (isRun.get() && resourceQueue.size() < max.get() && count > 0) {
            for (int i = 0; i < count; i++) {
                if (!add()) {
                    break;
                }
            }
        }
    }

    public void addResourceZeroPool() {
        if (min == 0 && resourceQueue.isEmpty() && parkQueue.isEmpty()) {
            add();
        }
    }

    // Этот метод нельзя вызывать под бизнес задачи, система сама должна это контролировать
    // Если ресурса нет - ждите
    // keepAlive работает на статистике от ресурсов. Если у пула min = 0, этот метод не поможет разогнать
    @Override
    public void keepAlive() {
        if (isRun.get()) {
            try {
                // Было изначально так: если на парковке никого нет - добавляем ресурс
                // Из-за того, что задача сбора статистики вызывается в параллель с keepAlive
                // Получается, что в паркинге нет никого и мы постоянно добавляем туда ресурс
                // Добавленный ресурс ни кем не используется и попадает под нож, постоянно увеличивая счётчик ресурсов
                // Поэтому добавили доп условие, что за промежуток времени не было complete со статусом isFinish = true
                // Переход на tpoParkQueue оказался провальным, так как иногда задачи выполняются реже чем keepAlive
                // Это приводит к тому, что tpoParkQueue может быть 0 без и аналогичная ситуация с паркингом при
                // одновременном выполнении задачи и keepAlive, когда парк пустой и tpoParkQueue = 0 и мы опять
                // идём в увеличении ресурсов.
                // Сейчас буду проигрывать историю, когда буду считать время когда парк опустел до момента, когда в парк
                // вернулись ресурсы
                // Всё стало хорошо, НО иногда в парк могут возвращаться ресурсы, которые вышли по состоянию
                // isOverflowIteration и по ним не надо считать сброс времени, что кол-во в парке стало больше 0 так как
                // это вынужденная мера
                // Вырезана все isFinish, так как целевое решение по контролю бесконечных задач
                // должно решаться в RateLimit
                if (getTimeWhenParkIsEmpty() > 1000 && parkQueue.isEmpty()) {
                    overclocking(formulaAddCount.apply(1));
                } else if (resourceQueue.size() > min) { //Кол-во больше минимума
                    removeLazy();
                }
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }
        // Тут происходит непосредственно удаление
        while (!removeQueue.isEmpty()) {
            T resource = removeQueue.pollLast();
            if (resource != null) {
                remove(resource);
            }
        }
        //Удаление ошибочных
        while (!exceptionQueue.isEmpty()) {
            T resource = exceptionQueue.pollLast();
            if (resource != null) {
                remove(resource);
            }
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addToRemove(T resource) {
        //Удалять ресурсы из вне можно только из паркинга, когда они отработали
        if (parkQueue.contains(resource)) {
            parkQueue.remove(resource);
            checkPark(); //Если сказали, что надо удалять, то наверное противится уже не стоит
        } else {
            return false;
        }
        return addToRemoveWithoutCheckParking(resource);
    }

    // Это не явное удаление, а всего лишь маркировка, что в принципе ресурс может быть удалён
    private boolean addToRemoveWithoutCheckParking(T resource) {
        // Выведено в асинхрон через keepAlive, потому что если ресурс - поток, то когда он себя возвращает
        // Механизм closeResource пытается завершить процесс, который ждёт выполнение этой команды
        // Грубо это deadLock получается без асинхрона
        if (resourceQueue.size() > min && !removeQueue.contains(resource)) {
            removeQueue.add(resource);
            return true;
        }
        return false;
    }

    private void removeLazy() { //Проверка ждунов, что они давно не вызывались
        if (isRun.get()) {
            final long curTimeMs = System.currentTimeMillis();
            final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
            //C конца будем пробегать
            Util.riskModifierCollection(null, parkQueue, getEmptyType(), (T resource) -> {
                if (maxCounterRemove.get() == 0) {
                    return;
                }
                if (!resource.isExpired(curTimeMs)) {
                    return;
                }
                if (addToRemove(resource)) {
                    maxCounterRemove.decrementAndGet();
                }
            }, true);
        }
    }

    @SuppressWarnings("unused")
    @Override
    public void run() {
        if (restartOperation.compareAndSet(false, true)) {
            isRun.set(true);
            for (int i = 0; i < min; i++) {
                add();
            }
            rateLimit.setActive(true);
            rateLimitPoolItem.setActive(true);
            restartOperation.set(false);
        }
    }

    @Override
    public void shutdown() {
        if (restartOperation.compareAndSet(false, true)) {
            isRun.set(false);
            Util.riskModifierCollection(
                    null,
                    resourceQueue,
                    getEmptyType(),
                    this::remove
            );
            rateLimit.setActive(false);
            rateLimitPoolItem.setActive(false);
            restartOperation.set(false);
        }
    }

    @Override
    synchronized public void reload() {
        shutdown();
        run();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(
            Map<String, String> parentTags,
            Map<String, Object> parentFields,
            AtomicBoolean isRun
    ) {
        List<Statistic> result = new ArrayList<>();
        if (resourceQueue.size() > 0 || parkQueue.size() > 0 || removeQueue.size() > 0) {
            active();
        }
        result.add(new Statistic(parentTags, parentFields)
                .addTag("Pool", getName())
                .addField("min", min)
                .addField("max", max)
                .addField("resource", resourceQueue.size())
                .addField("park", parkQueue.size())
                .addField("remove", removeQueue.size())
                .addField("sumTime", sumTime)
        );
        return result;
    }

}
