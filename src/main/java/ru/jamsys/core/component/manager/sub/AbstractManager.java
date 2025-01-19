package ru.jamsys.core.component.manager.sub;

import lombok.Setter;
import ru.jamsys.core.App;
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

// Менаджер объектов, которые могут прекращать свою жизнидеятельность по ExpirationMsMutable
// Если объектом не пользуются - он будет выпадать из жизни, и мы не будем тратить время на сбор статистики этого
// объекта + так же будем его останавливать (shutdown), но ссылку на объект будем прихранивать в mapReserved
// и при повторной попытке получить объект - мы его восттановим из mapReserved в map и запустим (run)

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

    private final Map<String, E> map = new ConcurrentHashMap<>();

    private final Map<String, E> mapReserved = new ConcurrentHashMap<>();

    @Setter
    protected boolean cleanableMap = true;

    private final Lock lockAddFromRemoved = new ReentrantLock();

    @Override
    public Map<String, E> getMapForFlushStatistic() {
        // Логичнее сделать было new HashMap<>(map)
        // но каждый раз копировать карту - такое себе, контролируйте вызов метода getMapForFlushStatistic
        // что бы там не было никаких корректировок карты - иначе могут быть непредвиденные обстоятельства
        return map;
    }

    @Override
    public void keepAlive(AtomicBoolean threadRun) {
        lockAddFromRemoved.lock();
        UtilRisc.forEach(
                threadRun,
                map,
                (String key, E element) -> {
                    if (cleanableMap && element.isExpiredWithoutStop()) {
                        mapReserved.put(key, map.remove(key));
                        element.shutdown();
                    } else if (element instanceof KeepAlive) {
                        ((KeepAlive) element).keepAlive(threadRun);
                    }
                }
        );
        lockAddFromRemoved.unlock();
    }

    public void checkReserved() {
        UtilRisc.forEach(new AtomicBoolean(true), mapReserved, (key, element) -> {
            if (!element.isExpiredWithoutStop()) {
                restoreFromReserved(key, null);
            }
        });
    }

    public E externalMapGet(String key) {
        return map.get(key);
    }

    public void externalMapPut(String key, E element) {
        if (element == null) {
            App.error(new RuntimeException("externalMapPut " + key + "; element is null"));
            return;
        }
        lockAddFromRemoved.lock();
        map.put(key, element);
        lockAddFromRemoved.unlock();
    }

    // Атомарная операция для map и mapReserved
    private E restoreFromReserved(String key, Supplier<E> supplierNewObject) {
        lockAddFromRemoved.lock();
        E result = null;
        // computeIfAbsent так как заметил использование в других классах использование put
        // 19.01.2025 сделал map приватным, полагаю put больше не должен использоваться в других классах
        // поэтому убрал реализацию computeIfAbsent
        // Проблема пошла, потому что появились null в map
        if (mapReserved.containsKey(key)) {
            result = mapReserved.remove(key);
            if (result == null) {
                App.error(new RuntimeException("mapReserved.remove(key), where key = " + key + "; return null"));
            } else {
                map.put(key, result);
                result.run();
            }
        } else if (map.containsKey(key)) {
            result = map.get(key);
        } else if (supplierNewObject != null) {
            result = supplierNewObject.get();
            if (result == null) {
                App.error(new RuntimeException("supplierNewObject.get() for key = " + key + " return null"));
            } else {
                map.put(key, result);
                result.run();
            }
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
