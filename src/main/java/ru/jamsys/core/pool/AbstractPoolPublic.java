package ru.jamsys.core.pool;

import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.util.concurrent.atomic.AtomicBoolean;

// Я считаю, что эта реализация плохая, так как в ней есть ожидания
// Я решил оставить эту реализацию только для тестирования, так как напрямую можно пощупать poolItem

public abstract class AbstractPoolPublic<RA, RR, PI extends ExpirationMsMutable & Resource<RA, RR>>
        extends AbstractPool<RA, RR, PI> {

    public AbstractPoolPublic(String name, Class<PI> cls) {
        super(name, cls);
    }

    public PI getPoolItem() {
        if (!isRun.get()) {
            return null;
        }
        // Забираем с начала, что бы под нож улетели последние добавленные
        PI poolItem = parkQueue.pollLast();
        if (poolItem != null) {
            updateParkStatistic();
        }
        return poolItem;
    }

    public PI getPoolItem(long timeOutMs, AtomicBoolean isThreadRun) {
        if (!isRun.get()) {
            return null;
        }
        long finishTimeMs = System.currentTimeMillis() + timeOutMs;
        while (isRun.get() && isThreadRun.get() && finishTimeMs > System.currentTimeMillis()) {
            PI poolItem = parkQueue.pollLast();
            if (poolItem != null) {
                updateParkStatistic();
                return poolItem;
            }
            Util.sleepMs(100);
        }
        return null;
    }

}
