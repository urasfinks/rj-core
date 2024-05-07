package ru.jamsys.core.pool;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.api.RateLimitManager;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.RunnableInterface;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.rate.limit.item.RateLimitItemInstance;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableImpl;
import ru.jamsys.core.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@ToString(onlyExplicitlyIncluded = true)
public abstract class AbstractPool<T extends PoolItem<T>>
        extends ExpiredMsMutableImpl implements Pool<T>, RunnableInterface, KeepAlive, ClassName {

    public static ThreadLocal<Pool<?>> context = new ThreadLocal<>();

    @Getter
    @ToString.Include
    public final String name;

    protected final ConcurrentLinkedDeque<T> parkQueue = new ConcurrentLinkedDeque<>();

    protected final ConcurrentLinkedDeque<T> removeQueue = new ConcurrentLinkedDeque<>();

    protected final ConcurrentLinkedDeque<T> exceptionQueue = new ConcurrentLinkedDeque<>();

    protected final ConcurrentLinkedDeque<T> itemQueue = new ConcurrentLinkedDeque<>();

    protected final AtomicBoolean isRun = new AtomicBoolean(false);

    @Getter
    protected final RateLimit rateLimit;

    @Getter
    protected final RateLimit rateLimitPoolItem;

    private final int min; //Минимальное кол-во ресурсов

    private final AtomicBoolean restartOperation = new AtomicBoolean(false);

    @Setter
    private Function<Integer, Integer> formulaAddCount = (need) -> need;

    @Setter
    private Function<Integer, Integer> formulaRemoveCount = (need) -> need;

    private long timeWhenParkIsEmpty = -1;

    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AtomicBoolean dynamicPollSize = new AtomicBoolean(false);

    private final RateLimitItem rliPoolSize;

    public AbstractPool(String name, int min, Class<T> cls) {
        this.name = name;
        this.min = min;
        RateLimitManager rateLimitManager = App.context.getBean(RateLimitManager.class);
        rateLimit = rateLimitManager.get(getClassName(name))
                .init(RateLimitName.POOL_SIZE.getName(), RateLimitItemInstance.MAX);
        rliPoolSize = rateLimit.get(RateLimitName.POOL_SIZE.getName());

        rateLimitPoolItem = rateLimitManager.get(ClassNameImpl.getClassNameStatic(cls, name));

    }

    public void setDynamicPollSize(boolean dynamic) {
        dynamicPollSize.set(dynamic);
    }

    public boolean allInPark() {
        return parkQueue.size() == itemQueue.size();
    }

    @SafeVarargs
    static <T> T[] getEmptyType(T... array) {
        return Arrays.copyOf(array, 0);
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
        sb.append("item: ").append(itemQueue.size()).append("; ");
        sb.append("park: ").append(parkQueue.size()).append("; ");
        sb.append("remove: ").append(removeQueue.size()).append("; ");
        sb.append("isRun: ").append(isRun.get()).append("; ");
        sb.append("min: ").append(min).append("; ");
        sb.append("max: ").append(rliPoolSize.getMax()).append("; ");
        //sb.append("timePark0: ").append(getTimeWhenParkIsEmpty()).append("; ");
        return sb.toString();
    }

    public boolean isAmI() {
        return this.equals(AbstractPool.context.get());
    }

    public boolean isEmpty() {
        return itemQueue.isEmpty();
    }

    public void setMaxSlowRiseAndFastFall(int max) {
        if (dynamicPollSize.get()) {
            if (max >= min) {
                if (max > rliPoolSize.getMax()) { //Медленно поднимаем
                    rliPoolSize.incrementMax();
                } else { //Но очень быстро опускаем
                    rliPoolSize.setMax(max);
                }
            } else {
                Util.logConsole("Pool [" + getName() + "] sorry max = " + max + " < Pool.min = " + min, true);
            }
        }
    }

    @SuppressWarnings("unused")
    @Override
    public void complete(T poolItem, Exception e) {
        if (poolItem == null) {
            return;
        }
        if (checkExceptionOnComplete(e)) {
            exceptionQueue.add(poolItem);
            return;
        }
        if (!rliPoolSize.check(itemQueue.size()) || !isRun.get()) {
            if (addToRemoveWithoutCheckParking(poolItem)) {
                return;
            }
        }
        // Если взять потоки как ресурсы, то после createPoolItem вызывается Thread.start, который после работы сам
        // себя вносит в пул отработанных (parkQueue)
        // Но вообще понятие ресурсов статично и они не живут собственной жизнью
        // поэтому после createPoolItem мы кладём их в parkQueue, что бы их могли взять желающие
        // И для потоков тут получается первичный дубль
        if (!parkQueue.contains(poolItem)) {
            parkQueue.addLast(poolItem);
            checkPark();
        }
    }

    @SuppressWarnings({"unused"})
    @Override
    public T getPoolItem() {
        if (!isRun.get()) {
            return null;
        }
        tpsDequeue.incrementAndGet();
        // Забираем с начала, что бы под нож улетели последние добавленные
        T poolItem = parkQueue.pollFirst();
        checkPark();
        if (poolItem != null) {
            poolItem.polled();
        }
        return poolItem;
    }

    @SuppressWarnings({"unused"})
    @Override
    public T getPoolItem(long timeOutMs, AtomicBoolean isThreadRun) {
        if (!isRun.get()) {
            return null;
        }
        tpsDequeue.incrementAndGet();
        long finishTimeMs = System.currentTimeMillis() + timeOutMs;
        while (isRun.get() && isThreadRun.get() && finishTimeMs > System.currentTimeMillis()) {
            T poolItem = parkQueue.pollFirst();
            checkPark();
            if (poolItem != null) {
                poolItem.polled();
                return poolItem;
            }
            Util.sleepMs(100);
        }
        return null;
    }

    private boolean add() {
        if (isRun.get() && rliPoolSize.check(itemQueue.size())) {
            if (!removeQueue.isEmpty()) {
                T poolItem = removeQueue.pollLast();
                if (poolItem != null) {
                    parkQueue.add(poolItem);
                    checkPark();
                    return true;
                }
            }
            final T poolItem = createPoolItem();
            if (poolItem != null) {
                //#1
                itemQueue.add(poolItem);
                //#2
                parkQueue.add(poolItem);
                checkPark();
                return true;
            }
        }
        return false;
    }

    // Бывают случаи когда что-то прекращает работать само собой и надо просто вырезать из пула ссылки
    public void remove(@NonNull T poolItem) {
        itemQueue.remove(poolItem);
        parkQueue.remove(poolItem);
        checkPark();
        removeQueue.remove(poolItem);
    }

    // А бывает когда надо удалить ссылки и закрыть ресурс по причине самого пула, что ресурс не нужен
    public void removeAndClose(@NonNull T poolItem) {
        remove(poolItem);
        closePoolItem(poolItem);
    }

    private void overclocking(int count) {
        if (isRun.get() && rliPoolSize.check(itemQueue.size()) && count > 0) {
            for (int i = 0; i < count; i++) {
                if (!add()) {
                    break;
                }
            }
        }
    }

    public void addPoolItemIfEmpty() {
        if (min == 0 && itemQueue.isEmpty() && parkQueue.isEmpty()) {
            add();
        }
    }

    // Этот метод нельзя вызывать под бизнес задачи, система сама должна это контролировать
    // Если ресурса нет - ждите
    // keepAlive работает на статистике от ресурсов. Если у пула min = 0, этот метод не поможет разогнать
    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
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
                } else if (itemQueue.size() > min) { //Кол-во больше минимума
                    removeLazy();
                }
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }
        // Тут происходит непосредственно удаление
        while (!removeQueue.isEmpty()) {
            T poolItem = removeQueue.pollLast();
            if (poolItem != null) {
                removeAndClose(poolItem);
            }
        }
        //Удаление ошибочных
        while (!exceptionQueue.isEmpty()) {
            T poolItem = exceptionQueue.pollLast();
            if (poolItem != null) {
                removeAndClose(poolItem);
            }
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addToRemove(T poolItem) {
        //Удалять ресурсы из вне можно только из паркинга, когда они отработали
        if (parkQueue.contains(poolItem)) {
            parkQueue.remove(poolItem);
            checkPark(); //Если сказали, что надо удалять, то наверное противится уже не стоит
        } else {
            return false;
        }
        return addToRemoveWithoutCheckParking(poolItem);
    }

    // Это не явное удаление, а всего лишь маркировка, что в принципе ресурс может быть удалён
    private boolean addToRemoveWithoutCheckParking(T poolItem) {
        // Выведено в асинхрон через keepAlive, потому что если ресурс - поток, то когда он себя возвращает
        // Механизм closePoolItem пытается завершить процесс, который ждёт выполнение этой команды
        // Грубо это deadLock получается без асинхрона
        if (itemQueue.size() > min && !removeQueue.contains(poolItem)) {
            removeQueue.add(poolItem);
            return true;
        }
        return false;
    }

    private void removeLazy() { //Проверка ждунов, что они давно не вызывались
        if (isRun.get()) {
            final long curTimeMs = System.currentTimeMillis();
            final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
            //C конца будем пробегать
            Util.riskModifierCollection(null, parkQueue, getEmptyType(), (T poolItem) -> {
                if (maxCounterRemove.get() == 0) {
                    return;
                }
                if (!poolItem.isExpired(curTimeMs)) {
                    return;
                }
                if (addToRemove(poolItem)) {
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
                    itemQueue,
                    getEmptyType(),
                    this::removeAndClose
            );
            rateLimit.setActive(false);
            rateLimitPoolItem.setActive(false);
            restartOperation.set(false);
        }
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        int tpsDequeueFlush = tpsDequeue.getAndSet(0);
        if (tpsDequeueFlush > 0) {
            active();
        }
        result.add(new Statistic(parentTags, parentFields)
                .addField("tpsDeq", tpsDequeueFlush)
                .addField("min", min).addField("max", rliPoolSize.getMax())
                .addField("item", itemQueue.size())
                .addField("park", parkQueue.size())
                .addField("remove", removeQueue.size())
        );
        return result;
    }

}
