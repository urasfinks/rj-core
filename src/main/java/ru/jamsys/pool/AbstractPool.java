package ru.jamsys.pool;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.jamsys.App;
import ru.jamsys.RunnableInterface;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


public abstract class AbstractPool<T> implements Pool<T>, RunnableInterface {

    @Getter
    private int max; //Максимальное кол-во ресурсов

    public void setMax(int max) {
        if (max >= min) {
            this.max = max;
        } else {
            Util.logConsole("Pool [" + getName() + "] sorry max = " + max + " < " + min);
        }
    }

    @Getter
    @Setter
    private int min; //Минимальное кол-во ресурсов

    private final long keepAliveMs; //Время жизни ресурса без работы

    @Getter
    private final String name;

    @Setter
    private Function<Integer, Integer> formulaAddCount = (need) -> need;

    @Setter
    private Function<Integer, Integer> formulaRemoveCount = (need) -> need;

    protected final ConcurrentLinkedDeque<ResourceEnvelope<T>> parkQueue = new ConcurrentLinkedDeque<>();

    protected final ConcurrentLinkedDeque<ResourceEnvelope<T>> removeQueue = new ConcurrentLinkedDeque<>();

    protected final Map<T, ResourceEnvelope<T>> map = new ConcurrentHashMap<>();

    protected final AtomicBoolean isRun = new AtomicBoolean(false);

    private final AtomicBoolean restartOperation = new AtomicBoolean(false);

    public AbstractPool(String name, int min, int max, long keepAliveMs) {
        this.name = name;
        this.max = max;
        this.min = min;
        this.keepAliveMs = keepAliveMs;
    }

    public boolean isAllInPark() {
        return isRun.get() && map.size() == parkQueue.size();
    }

    @SuppressWarnings("unused")
    @Override
    public void complete(T resource, Exception e) {
        if (resource == null) {
            return;
        }
        ResourceEnvelope<T> resourceEnvelope = map.get(resource);
        if (resourceEnvelope == null) {
            App.context.getBean(ExceptionHandler.class).handler(new Exception("Не найдена обёртка в пуле " + resource));
            return;
        }
        if (map.size() > max || !isRun.get() || checkExceptionOnComplete(e)) {
            if (map.size() > min) {
                if (!removeQueue.contains(resourceEnvelope)) {
                    // Выведено в асинхрон через keepAlive, потому что если ресурс - поток, то когда он себя возвращает
                    // Механизм closeResource пытается завершить процесс, который ждёт выполнение этой команды
                    // Грубо это deadLock получается без асинхрона
                    removeQueue.add(resourceEnvelope);
                }
                return;
            }
        }
        // Если взять потоки как ресурсы, то после createResource вызывается Thread.start, который после работы сам
        // себя вносит в пул отработанных (parkQueue)
        // Но вообще понятие ресурсов статично и они не живут собственной жизнью
        // поэтому после createResource мы кладём их в parkQueue, что бы их могли взять желающие
        // И для потоков тут получается первичный дубль
        if (!parkQueue.contains(resourceEnvelope)) {
            parkQueue.add(resourceEnvelope);
        }
    }

    @SuppressWarnings({"unused"})
    @Override
    public T getResource(Long timeOutMs) throws Exception {
        if (!isRun.get()) {
            throw new Exception("Pool " + getName() + " stop.");
        }
        if (timeOutMs == null) {
            ResourceEnvelope<T> resourceEnvelope = parkQueue.pollLast();
            if (resourceEnvelope != null) {
                resourceEnvelope.setLastRunMs(System.currentTimeMillis());
                return resourceEnvelope.getResource();
            }
        } else {
            long finishTimeMs = System.currentTimeMillis() + timeOutMs;
            while (isRun.get() && finishTimeMs > System.currentTimeMillis()) {
                ResourceEnvelope<T> resourceEnvelope = parkQueue.pollLast();
                if (resourceEnvelope != null) {
                    resourceEnvelope.setLastRunMs(System.currentTimeMillis());
                    return resourceEnvelope.getResource();
                }
                Util.sleepMs(100);
            }
            throw new Exception("Pool " + getName() + " not active resource. Timeout: " + timeOutMs + "ms");
        }
        return null;
    }

    @SafeVarargs
    static <T> T[] getEmptyType(T... array) {
        return Arrays.copyOf(array, 0);
    }

    private boolean add() {
        if (isRun.get() && map.size() < max) {
            if (!removeQueue.isEmpty()) {
                ResourceEnvelope<T> resourceEnvelope = removeQueue.pollLast();
                if (resourceEnvelope != null) {
                    parkQueue.add(resourceEnvelope);
                    return true;
                }
            }
            final ResourceEnvelope<T> resourceEnvelope = new ResourceEnvelope<>(createResource());
            if (resourceEnvelope.getResource() != null) {
                parkQueue.add(resourceEnvelope);
                map.put(resourceEnvelope.getResource(), resourceEnvelope);
                return true;
            }
        }
        return false;
    }

    synchronized private void remove(@NonNull ResourceEnvelope<T> resourceEnvelope) {
        map.remove(resourceEnvelope.getResource());
        parkQueue.remove(resourceEnvelope); // На всякий случай
        closeResource(resourceEnvelope.getResource()); //Если выкидываем из пула, то наверное надо закрыть сам ресурс
    }

    private void overclocking(int count) {
        if (isRun.get() && map.size() < max && count > 0) {
            for (int i = 0; i < count; i++) {
                if (!add()) {
                    break;
                }
            }
        }
    }

    @Override
    public void keepAlive() {
        if (isRun.get()) {
            try {
                if (parkQueue.isEmpty()) { //Если в очереди пустота, попробуем добавить
                    overclocking(formulaAddCount.apply(1));
                } else if (map.size() > min) { //Кол-во больше минимума
                    removeLazy();
                }
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }
        // Тут происходит непосредственно удаление
        shutdownRemoveThread();
    }

    private void shutdownRemoveThread() {
        while (!removeQueue.isEmpty()) {
            ResourceEnvelope<T> resourceEnvelope = removeQueue.pollLast();
            if (resourceEnvelope != null) {
                remove(resourceEnvelope);
            }
        }
    }

    private void removeLazy() { //Проверка ждунов, что они давно не вызывались и у них кол-во итераций равно 0 -> нож
        if (isRun.get()) {
            final long curTimeMs = System.currentTimeMillis();
            final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
            Util.riskModifierMap(null, map, getEmptyType(), (T key, ResourceEnvelope<T> resourceEnvelope) -> {
                if (maxCounterRemove.get() == 0) {
                    return;
                }
                //Время последнего оживления превысило keepAlive + мы не превысили кол-во удалений за 1 проверку
                if (curTimeMs < (resourceEnvelope.getLastRunMs() + keepAliveMs)) {
                    return;
                }
                if (map.size() > min) {
                    if (!removeQueue.contains(resourceEnvelope)) {
                        removeQueue.add(resourceEnvelope);
                    }
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
            Util.riskModifierMap(
                    null,
                    map,
                    getEmptyType(),
                    (T key, ResourceEnvelope<T> resourceEnvelope) -> {
                        closeResource(resourceEnvelope.getResource());
                        map.remove(key);
                        parkQueue.remove(resourceEnvelope);
                    }
            );
            restartOperation.set(false);
        }
    }

    @Override
    synchronized public void reload() {
        shutdown();
        run();
    }

    @SuppressWarnings("unused")
    public List<Statistic> flushAndGetStatistic(
            Map<String, String> parentTags,
            Map<String, Object> parentFields,
            AtomicBoolean isRun
    ) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addTag("index", getName())
                .addField("min", min)
                .addField("max", max)
                .addField("size", map.size())
                .addField("park", parkQueue.size())
        );
        return result;
    }

}
