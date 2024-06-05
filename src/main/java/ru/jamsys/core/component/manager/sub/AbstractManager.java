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

// MO - MapObject
// BA - BuilderArgument

public abstract class AbstractManager<
        MO extends
                Closable
                & ExpirationMsMutable
                & StatisticsFlush
                & ManagerItemAutoRestore
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
                    if (cleanableMap && element.isExpired()) {
                        mapReserved.put(key, map.remove(key));
                        element.close();
                    } else if (element instanceof KeepAlive) {
                        ((KeepAlive) element).keepAlive(isThreadRun);
                    }
                }
        );
        lockAddFromRemoved.unlock();
    }

    // Скрытая реализация, потому что объекты могут выпадать из общей Map так как у них есть срок жизни
    // Они унаследованы от ExpirationMsMutable, если реализация будет открытой, кто-то может прихранить ссылки на
    // текущий брокер по ключу, а он в какой-то момент времени может быть удалён, а ссылка останется
    // мы будем накладывать в некую очередь, которая уже будет не принадлежать менаджеру
    // и обслуживаться тоже не будет [keepAlive, flushAndGetStatistic] так что - плохая эта затея

    private final Lock lockAddFromRemoved = new ReentrantLock();

    protected MO getManagerElement(String key, Class<?> classItem, BA builderArgument) {
        MO o = map.computeIfAbsent(key, k1 -> {
            MO build;
            lockAddFromRemoved.lock();
            build = mapReserved.containsKey(k1) ? mapReserved.remove(k1): build(key, classItem, builderArgument);
            lockAddFromRemoved.unlock();
            return build;
        });
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
