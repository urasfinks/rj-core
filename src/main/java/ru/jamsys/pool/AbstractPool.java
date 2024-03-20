package ru.jamsys.pool;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.extension.Procedure;
import ru.jamsys.extension.RunnableInterface;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.AbstractExpired;
import ru.jamsys.statistic.Expired;
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
public abstract class AbstractPool<T extends Expired> extends AbstractExpired implements Pool<T>, RunnableInterface, StatisticsCollector {

    public static ThreadLocal<Pool<?>> userContext = new ThreadLocal<>();

    private final AtomicInteger max = new AtomicInteger(0); //Максимальное кол-во ресурсов

    private final int min; //Минимальное кол-во ресурсов

    @Getter
    private final List<Procedure> listProcedureOnShutdown = new ArrayList<>();

    @Setter
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

    protected final ConcurrentLinkedDeque<T> resourceQueue = new ConcurrentLinkedDeque<>();

    protected final AtomicBoolean isRun = new AtomicBoolean(false);

    private final AtomicBoolean restartOperation = new AtomicBoolean(false);

    public AbstractPool(String name, int min, int max) {
        this.name = name;
        this.max.set(max);
        this.min = min;
    }

    public boolean isAmI() {
        return this.equals(AbstractPool.userContext.get());
    }

    public boolean isEmpty() {
        return resourceQueue.isEmpty();
    }

    public void setMaxSlowRiseAndFastFall(int max) {
        if (max >= min) {
            if (max > this.max.get()) { //Медленно поднимаем
                this.max.incrementAndGet();
            } else { //Но очень быстро опускаем
                this.max.set(max);
            }
        } else {
            Util.logConsole("Pool [" + getName() + "] sorry max = " + max + " < " + min);
        }
    }

    @SuppressWarnings("unused")
    @Override
    public void complete(T resource, Exception e) {
        if (resource == null) {
            return;
        }
        if (resourceQueue.size() > max.get() || !isRun.get() || checkExceptionOnComplete(e)) {
            if (addToRemove(resource)) {
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
        }
    }

    @SuppressWarnings({"unused"})
    @Override
    public T getResource() {
        if (!isRun.get()) {
            return null;
        }
        // Забираем с конца, что бы под нож первые улетели
        return parkQueue.pollLast();
    }

    @SuppressWarnings({"unused"})
    @Override
    public T getResource(Long timeOutMs) {
        if (!isRun.get()) {
            return null;
        }
        long finishTimeMs = System.currentTimeMillis() + timeOutMs;
        while (isRun.get() && finishTimeMs > System.currentTimeMillis()) {
            T resource = parkQueue.pollLast();
            if (resource != null) {
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
                    return true;
                }
            }
            final T resource = createResource();
            if (resource != null) {
                //#1
                resourceQueue.add(resource);
                //#2
                parkQueue.add(resource);
                return true;
            }
        }
        return false;
    }

    synchronized private void remove(@NonNull T resource) {
        resourceQueue.remove(resource);
        parkQueue.remove(resource); // На всякий случай
        removeQueue.remove(resource); // На всякий случай
        closeResource(resource); //Если выкидываем из пула, то наверное надо закрыть сам ресурс
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

    // Этот метод нельзя вызывать под бизнес задачи, система сама должна это контролировать
    // Если ресурса нет - ждите
    @Override
    public void keepAlive() {
        if (isRun.get()) {
            try {
                if (parkQueue.isEmpty()) { //Если в очереди пустота, попробуем добавить
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
    }

    private boolean addToRemove(T resource) {
        // Выведено в асинхрон через keepAlive, потому что если ресурс - поток, то когда он себя возвращает
        // Механизм closeResource пытается завершить процесс, который ждёт выполнение этой команды
        // Грубо это deadLock получается без асинхрона
        if (resourceQueue.size() > min) {
            if (!removeQueue.contains(resource)) {
                removeQueue.add(resource);
                return true;
            }
        }
        return false;
    }

    private void removeLazy() { //Проверка ждунов, что они давно не вызывались
        if (isRun.get()) {
            final long curTimeMs = System.currentTimeMillis();
            final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
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
            });
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
            listProcedureOnShutdown.forEach(Procedure::run);
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
                .addTag("index", getName())
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
