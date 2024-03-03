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

    public AbstractPool(String name, int min, int max, long keepAliveMillis) {
        this.name = name;
        this.max = max;
        this.min = min;
        this.keepAlive = keepAliveMillis;
    }

    @Setter
    private Function<Integer, Integer> formulaAddCount = (need) -> need;

    @Setter
    private Function<Integer, Integer> formulaRemoveCount = (need) -> need;

    private ConcurrentLinkedDeque<WrapResource<T>> parkQueue = new ConcurrentLinkedDeque<>();
    protected final Map<T, WrapResource<T>> map = new ConcurrentHashMap<>();

    protected final AtomicBoolean active = new AtomicBoolean(true);

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addTag("index", getName())
                .addField("size", map.size())
                .addField("park", parkQueue.size())
        );
        return result;
    }

    @SuppressWarnings("unused")
    public void complete(T ret, Exception e) {
        if (ret == null) {
            return;
        }
        if (active.get()) {
            WrapResource<T> wrapResource = map.get(ret);
            if (wrapResource != null) {
                if (checkExceptionOnRemove(e)) {
                    remove(wrapResource);
                } else {
                    parkQueue.add(wrapResource);
                }
            } else {
                new Exception("Не найдена обёртка в пуле " + ret).printStackTrace();
            }
        }
    }

    @SuppressWarnings({"unused"})
    public T getResource() throws Exception {
        int countMax = 10;
        int count = 0;
        int timeOut = 100;
        while (active.get()) {
            WrapResource<T> wrapResource = parkQueue.pollLast();
            if (wrapResource != null) {
                wrapResource.setLastRun(System.currentTimeMillis());
                return wrapResource.getResource();
            }
            Util.sleepMillis(timeOut);
            if (count++ > countMax) {
                break;
            }
        }
        throw new Exception("Pool " + getName() + " not active resource. Timeout: " + (timeOut * count) + "ms");
    }

    @SafeVarargs
    static <T> T[] getEmptyType(T... array) {
        return Arrays.copyOf(array, 0);
    }

    private void checkKeepAliveAndRemove() { //Проверка ждунов, что они давно не вызывались и у них кол-во итераций равно 0 -> нож
        if (active.get()) {
            final long curTimeMillis = System.currentTimeMillis();
            final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
            Util.riskModifierMap(null, map, getEmptyType(), (T key, WrapResource<T> value) -> {
                long future = value.getLastRun() + keepAlive;
                //Время последнего оживления превысило keepAlive + мы не привысили кол-во удалений за 1 проверку
                if (curTimeMillis > future && maxCounterRemove.getAndDecrement() > 0) {
                    safeRemove(value);
                }
            });
        }
    }

    private void safeRemove(WrapResource<T> wrapConnect) {
        if (map.size() > min) {
            remove(wrapConnect);
        }
    }

    synchronized private void remove(@NonNull WrapResource<T> wrapObject) {
        map.remove(wrapObject.getResource());
        parkQueue.remove(wrapObject); // На всякий случай
    }

    private boolean add() {
        if (active.get() && map.size() < max) {
            final WrapResource<T> wrapResource = new WrapResource<>();
            T resource = createResource();
            if (resource != null) {
                wrapResource.setResource(resource);
                parkQueue.add(wrapResource);
                map.put(wrapResource.getResource(), wrapResource);
                return true;
            }
        }
        return false;
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

    @SuppressWarnings("unused")
    public void initial() {
        for (int i = 0; i < min; i++) {
            add();
        }
    }

    public void stabilizer() {
        if (active.get()) {
            try {
                if (parkQueue.isEmpty()) { //Если в очереди пустота, попробуем добавить один
                    overclocking(formulaAddCount.apply(1));
                } else if (map.size() > min) { //Кол-во потоков больше минимума
                    checkKeepAliveAndRemove();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void shutdown() {
        active.set(false);
        Util.riskModifierMap(null, map, getEmptyType(), (T key, WrapResource<T> value) -> closeResource(value.getResource()));
    }

}
