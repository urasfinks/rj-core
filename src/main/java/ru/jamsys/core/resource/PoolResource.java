package ru.jamsys.core.resource;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.CheckClassItem;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

public class PoolResource<
        RC,
        RA,
        RR,
        PI extends Completable & ExpirationMsMutable & Resource<RC, RA, RR>
        >
        extends AbstractPoolResource<RC, RA, RR, PI>
        implements Closable, CheckClassItem {

    private final Class<PI> cls;

    public PoolResource(String name, Class<PI> cls) {
        super(name, cls);
        this.cls = cls;
    }

    @Override
    public PI createPoolItem() {
        return App.context.getBean(cls);
    }

    @Override
    public void closePoolItem(PI poolItem) {
        poolItem.close();
    }

    @Override
    public boolean checkExceptionOnComplete(Exception e) {
        return false;
    }

    @Override
    public void close() {
        shutdown();
    }

    @Override
    public boolean checkClassItem(Class<?> classItem) {
        return true;
    }

}
