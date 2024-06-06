package ru.jamsys.core.component.manager.sub;

import lombok.Setter;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

// MO - MapObject
// BA - BuilderArgument

public abstract class AbstractManager<
        MO extends
                Closable
                & ExpirationMsMutable
                & StatisticsFlush
                & CheckClassItem,
        BA>
        implements
        StatisticsCollectorMap<MO>,
        KeepAlive,
        StatisticsFlushComponent,
        ManagerElementBuilder<MO, BA> {

    protected final Map<String, MO> map = new ConcurrentHashMap<>();

    protected final Map<String, MO> mapReserved = new ConcurrentHashMap<>();

    @Setter
    protected boolean cleanableMap = true;

    private final Lock lockAddFromRemoved = new ReentrantLock();

    @Override
    public Map<String, MO> getMapForFlushStatistic() {
        return map;
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        lockAddFromRemoved.lock();
        UtilRisc.forEach(
                isThreadRun,
                map,
                (String key, MO element) -> {
                    if (cleanableMap && element.isExpiredWithoutStop()) {
                        mapReserved.put(key, map.remove(key));
                        element.close();
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
    private MO restoreFromReserved(String key, Supplier<MO> newObject) {
        lockAddFromRemoved.lock();
        MO result = null;
        // computeIfAbsent так как заметил использование в других классах использование put
        // хотя бы тут попробуем выдерживать консистентность
        if (mapReserved.containsKey(key)) {
            result = map.computeIfAbsent(key, mapReserved::remove);
        } else if (newObject != null) {
            result = map.computeIfAbsent(key, _ -> newObject.get());
        }
        lockAddFromRemoved.unlock();
        return result;
    }

    // Скрытая реализация, потому что объекты могут выпадать из общей Map так как у них есть срок жизни
    // Они унаследованы от ExpirationMsMutable, если реализация будет открытой, кто-то может прихранить ссылки на
    // текущий брокер по ключу, а он в какой-то момент времени может быть удалён, а ссылка останется
    // мы будем накладывать в некую очередь, которая уже будет не принадлежать менаджеру
    // и обслуживаться тоже не будет [keepAlive, flushAndGetStatistic] так что - плохая эта затея
    protected MO getManagerElement(String key, Class<?> classItem, BA builderArgument) {
        MO o = restoreFromReserved(key, () -> build(key, classItem, builderArgument));
        if (o != null && o.checkClassItem(classItem)) {
            return o;
        }
        return null;
    }

    protected MO getManagerElementUnsafe(String key, Class<?> classItem) {
        MO o = map.get(key);
        if (o != null && o.checkClassItem(classItem)) {
            return o;
        }
        return null;
    }

    public Map<String, MO> getTestMap() {
        return new HashMap<>(map);
    }

}
