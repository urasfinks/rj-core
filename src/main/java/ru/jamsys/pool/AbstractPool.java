package ru.jamsys.pool;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.jamsys.Util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Data
public abstract class AbstractPool<T> implements Pool<T> {

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

    private ConcurrentLinkedDeque<PoolResource<T>> parkQueue = new ConcurrentLinkedDeque<>();
    protected final Map<T, PoolResource<T>> map = new ConcurrentHashMap<>();

    protected final AtomicInteger tpsAdd = new AtomicInteger(0);
    protected final AtomicInteger tpsGet = new AtomicInteger(0);
    protected final AtomicInteger tpsParkIn = new AtomicInteger(0);
    protected final ConcurrentLinkedQueue<PoolResource<T>> tpsRemove = new ConcurrentLinkedQueue<>();
    protected final AtomicBoolean active = new AtomicBoolean(true);

    protected final PoolStatisticData statLastSec = new PoolStatisticData(); //Агрегированная статистика за прошлый период (сейчас 1 секунда)

    public PoolStatisticData flushStatistic() {
        statLastSec.setName(getName());
        statLastSec.setList(map.size());
        statLastSec.setPark(parkQueue.size());
        statLastSec.setTpsParkIn(tpsParkIn.getAndSet(0));
        statLastSec.setTpsAdd(tpsAdd.getAndSet(0));
        statLastSec.setTpsRemove(tpsRemove.size());
        tpsRemove.clear();
        return statLastSec;
    }

    @SuppressWarnings("unused")
    public void complete(T ret, Exception e) {
        if (ret == null) {
            return;
        }
        if (active.get()) {
            PoolResource<T> poolResource = map.get(ret);
            if (poolResource != null) {
                if (checkExceptionOnRemove(e)) {
                    remove(poolResource);
                } else {
                    tpsParkIn.incrementAndGet();
                    parkQueue.add(poolResource);
                }
            } else {
                new Exception("Не найдена обёртка в пуле " + ret.toString()).printStackTrace();
            }
        }
    }

    @SuppressWarnings({"unused"})
    public T getResource() throws Exception {
        while (active.get()) {
            PoolResource<T> poolResource = parkQueue.pollLast();
            if (poolResource != null) {
                tpsGet.incrementAndGet();
                poolResource.setLastRun(System.currentTimeMillis());
                return poolResource.getResource();
            }
            Util.sleepMillis(100);
        }
        throw new Exception("Pool " + getName() + " not active");
    }

    private void checkKeepAliveAndRemove() { //Проверка ждунов, что они давно не вызывались и у них кол-во итераций равно 0 -> нож
        if (active.get()) {
            try {
                final long curTimeMillis = System.currentTimeMillis();
                final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
                Object[] objects = map.keySet().toArray();
                for (Object object : objects) {
                    PoolResource<T> poolResource = map.get(object);
                    long future = poolResource.getLastRun() + keepAlive;
                    //Время последнего оживления превысило keepAlive + поток реально не работал
                    if (curTimeMillis > future && maxCounterRemove.getAndDecrement() > 0) {
                        safeRemove(poolResource);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void safeRemove(PoolResource<T> wrapConnect) {
        if (map.size() > min) {
            remove(wrapConnect);
        }
    }

    synchronized private void remove(@NonNull PoolResource<T> wrapObject) {
        map.remove(wrapObject.getResource());
        parkQueue.remove(wrapObject); // На всякий случай
        if (!tpsRemove.contains(wrapObject)) {
            tpsRemove.add(wrapObject);
        }
    }

    private boolean add() {
        if (active.get() && map.size() < max) {
            final PoolResource<T> poolResource = new PoolResource<>();
            T resource = createResource();
            if (resource != null) {
                poolResource.setResource(resource);

                parkQueue.add(poolResource);
                map.put(poolResource.getResource(), poolResource);

                tpsAdd.incrementAndGet();
                tpsParkIn.incrementAndGet();

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

    public void initial() {
        for (int i = 0; i < min; i++) {
            add();
        }
    }

    public void stabilizer() {
        if (active.get()) {
            try {
                if (parkQueue.isEmpty()) {
                    overclocking(formulaAddCount.apply(map.size()));
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
        Object[] objects = map.keySet().toArray();
        for (Object object : objects) {
            PoolResource<T> tPoolResource = map.get(object);
            closeResource(tPoolResource.getResource());
        }
    }

}
