package ru.jamsys.core.pool;

import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.util.concurrent.atomic.AtomicBoolean;

// Я считаю, что эта реализация плохая, так как в ней есть ожидания
// Я решил оставить эту реализацию только для тестирования, так как напрямую можно пощупать poolItem

public abstract class AbstractPoolPublic<RA, RR, PI extends ExpirationMsMutable & Resource<RA, RR>>
        extends AbstractPool<RA, RR, PI> {

    public AbstractPoolPublic(String name) {
        super(name);
    }

    public PI getPoolItem() {
        if (!run.get()) {
            return null;
        }
        return getFromPark();
    }

    public PI getPoolItem(long timeOutMs, AtomicBoolean threadRun) {
        if (!run.get()) {
            return null;
        }
        long finishTimeMs = System.currentTimeMillis() + timeOutMs;
        while (run.get() && threadRun.get() && finishTimeMs > System.currentTimeMillis()) {
            PI poolItem = getFromPark();
            if (poolItem != null) {
                return poolItem;
            }
            Util.sleepMs(100);
        }
        return null;
    }

}
