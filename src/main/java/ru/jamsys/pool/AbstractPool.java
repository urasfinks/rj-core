package ru.jamsys.pool;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticsCollector;
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

@Data
public abstract class AbstractPool<T> implements Pool<T>, StatisticsCollector {

    private final int max; //Максимальное кол-во коннектов
    private final int min; //Минимальное кол-во коннектов
    private final long keepAlive; //Время жизни коннекта без работы

    @Getter
    private final String name;

    public AbstractPool(String name, int min, int max, long keepAliveMs) {
        this.name = name;
        this.max = max;
        this.min = min;
        this.keepAlive = keepAliveMs;
    }

    @Setter
    private Function<Integer, Integer> formulaAddCount = (need) -> need;

    @Setter
    private Function<Integer, Integer> formulaRemoveCount = (need) -> need;

    private ConcurrentLinkedDeque<ResourceEnvelope<T>> parkQueue = new ConcurrentLinkedDeque<>();
    protected final Map<T, ResourceEnvelope<T>> map = new ConcurrentHashMap<>();

    protected final AtomicBoolean active = new AtomicBoolean(true);

    @SuppressWarnings("unused")
    @Override
    public void complete(T ret, Exception e) {
        if (ret == null) {
            return;
        }
        if (active.get()) {
            ResourceEnvelope<T> resourceEnvelope = map.get(ret);
            if (resourceEnvelope != null) {
                if (checkExceptionOnComplete(e)) {
                    remove(resourceEnvelope);
                } else {
                    parkQueue.add(resourceEnvelope);
                }
            } else {
                new Exception("Не найдена обёртка в пуле " + ret).printStackTrace();
            }
        }
    }

    @SuppressWarnings({"unused"})
    @Override
    public T getResource(long timeOutMs) throws Exception {
        long finishTimeMs = System.currentTimeMillis() + timeOutMs;
        while (active.get() && finishTimeMs > System.currentTimeMillis()) {
            ResourceEnvelope<T> resourceEnvelope = parkQueue.pollLast();
            if (resourceEnvelope != null) {
                resourceEnvelope.setLastRunMs(System.currentTimeMillis());
                return resourceEnvelope.getResource();
            }
            Util.sleepMs(100);
        }
        throw new Exception("Pool " + getName() + " not active resource. Timeout: " + timeOutMs + "ms");
    }

    @SafeVarargs
    static <T> T[] getEmptyType(T... array) {
        return Arrays.copyOf(array, 0);
    }

    private boolean add() {
        if (active.get() && map.size() < max) {
            final ResourceEnvelope<T> resourceEnvelope = new ResourceEnvelope<>();
            T resource = createResource();
            if (resource != null) {
                resourceEnvelope.setResource(resource);
                parkQueue.add(resourceEnvelope);
                map.put(resourceEnvelope.getResource(), resourceEnvelope);
                return true;
            }
        }
        return false;
    }

    private void removeLazy() { //Проверка ждунов, что они давно не вызывались и у них кол-во итераций равно 0 -> нож
        if (active.get()) {
            final long curTimeMs = System.currentTimeMillis();
            final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
            Util.riskModifierMap(null, map, getEmptyType(), (T key, ResourceEnvelope<T> resourceEnvelope) -> {
                long future = resourceEnvelope.getLastRunMs() + keepAlive;
                //Время последнего оживления превысило keepAlive + мы не привысили кол-во удалений за 1 проверку
                if (curTimeMs > future && maxCounterRemove.getAndDecrement() > 0) {
                    remove(resourceEnvelope);
                }
            });
        }
    }

    synchronized private void remove(@NonNull ResourceEnvelope<T> resourceEnvelope) {
        if (map.size() > min) {
            map.remove(resourceEnvelope.getResource());
            parkQueue.remove(resourceEnvelope); // На всякий случай
        }
    }

    private void overclocking(int count) {
        if (active.get()) {
            if (map.size() >= max) {
                return;
            }
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    if (!add()) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void stabilizer() {
        if (active.get()) {
            try {
                if (parkQueue.isEmpty()) { //Если в очереди пустота, попробуем добавить один
                    overclocking(formulaAddCount.apply(1));
                } else if (map.size() > min) { //Кол-во потоков больше минимума
                    removeLazy();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unused")
    @Override
    public void run() {
        for (int i = 0; i < min; i++) {
            add();
        }
    }

    @Override
    public void shutdown() {
        active.set(false);
        Util.riskModifierMap(null, map, getEmptyType(), (T key, ResourceEnvelope<T> value) -> closeResource(value.getResource()));
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
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
