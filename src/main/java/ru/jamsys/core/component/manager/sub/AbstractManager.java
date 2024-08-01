package ru.jamsys.core.component.manager.sub;

import lombok.Setter;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

// E - Element
// EBA - ElementBuilderArgument

public abstract class AbstractManager<
        E extends
                ExpirationMsMutable
                & StatisticsFlush
                & LifeCycleInterface
                & ClassEquals,
        EBA>
        implements
        StatisticsCollectorMap<E>,
        KeepAlive,
        StatisticsFlushComponent,
        LifeCycleComponent,
        ManagerElementBuilder<E, EBA> {

    protected final Map<String, E> map = new ConcurrentHashMap<>();

    protected final Map<String, E> mapReserved = new ConcurrentHashMap<>();

    @Setter
    protected boolean cleanableMap = true;

    private final Lock lockAddFromRemoved = new ReentrantLock();

    @Override
    public Map<String, E> getMapForFlushStatistic() {
        return map;
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        lockAddFromRemoved.lock();
        UtilRisc.forEach(
                isThreadRun,
                map,
                (String key, E element) -> {
                    if (cleanableMap && element.isExpiredWithoutStop()) {
                        mapReserved.put(key, map.remove(key));
                        element.shutdown();
                    } else if (element instanceof KeepAlive) {
                        ((KeepAlive) element).keepAlive(isThreadRun);
                    }
                }
        );
        lockAddFromRemoved.unlock();
    }

    public void checkReserved() {
        for (String key : mapReserved.keySet()) {
            if (!mapReserved.get(key).isExpiredWithoutStop()) {
                restoreFromReserved(key, null);
            }
        }
    }

    // Атомарная операция для map и mapReserved
    private E restoreFromReserved(String key, Supplier<E> supplierNewObject) {
        lockAddFromRemoved.lock();
        E result = null;
        // computeIfAbsent так как заметил использование в других классах использование put
        // хотя бы тут попробуем выдерживать консистентность
        if (mapReserved.containsKey(key)) {
            result = map.computeIfAbsent(key, mapReserved::remove);
            result.run();
        } else if (map.containsKey(key)) {
            result = map.get(key);
        } else if (supplierNewObject != null) {
            result = map.computeIfAbsent(key, _ -> {
                E readyInstance = supplierNewObject.get();
                readyInstance.run();
                return readyInstance;
            });
        }
        lockAddFromRemoved.unlock();
        return result;
    }

    // Скрытая реализация, потому что объекты могут выпадать из общей Map так как у них есть срок жизни
    // Они унаследованы от ExpirationMsMutable, если реализация будет открытой, кто-то может прихранить ссылки на
    // текущий брокер по ключу, а он в какой-то момент времени может быть удалён, а ссылка останется
    // мы будем накладывать в некую очередь, которая уже будет не принадлежать менаджеру
    // и обслуживаться тоже не будет [keepAlive, flushAndGetStatistic] так что - плохая эта затея
    protected E getManagerElement(String key, Class<?> classItem, EBA builderArgument) {
        E element = restoreFromReserved(key, () -> build(key, classItem, builderArgument));
        if (element != null && element.classEquals(classItem)) {
            return element;
        }
        return null;
    }

    protected E getManagerElementUnsafe(String key, Class<?> classItem) {
        E element = map.get(key);
        if (element != null && element.classEquals(classItem)) {
            return element;
        }
        return null;
    }

    @Override
    public void shutdown() {
        UtilRisc.forEach(new AtomicBoolean(true), map, (String _, E element) -> element.shutdown());
        UtilRisc.forEach(new AtomicBoolean(true), mapReserved, (String _, E element) -> element.shutdown());
    }

    @Override
    public void run() {
        UtilRisc.forEach(new AtomicBoolean(true), map, (String _, E element) -> element.run());
        UtilRisc.forEach(new AtomicBoolean(true), mapReserved, (String _, E element) -> element.run());
    }

}
