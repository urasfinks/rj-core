package ru.jamsys.core.resource;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.PoolResourceCustomArgument;
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

    private final PoolResourceCustomArgument<PI, RC> cls;

    public PoolResource(String name, PoolResourceCustomArgument<PI, RC> cls) {
        super(name, cls.getCls());
        this.cls = cls;
    }

    @Override
    public PI createPoolItem() {
        PI newPoolItem = App.context.getBean(cls.getCls());
        newPoolItem.constructor(cls.getResourceConstructor());
        return newPoolItem;
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
