package ru.jamsys.core.resource;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

public class PoolResource<RA, RR, PI extends Completable & ExpirationMsMutable & Resource<RA, RR>> extends AbstractPoolResource<RA, RR, PI>{

    private final Class<PI> cls;

    public PoolResource(String name, int min, Class<PI> cls) {
        super(name, min, cls);
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

}
