package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.resource.PoolResource;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PoolResourceManager<RA, RR, PI extends Completable & ExpirationMsMutable & Resource<RA, RR>> extends AbstractManager<PoolResource<?, ?, ?>> {

    Map<String, Class<PI>> map = new ConcurrentHashMap<>();

    public ManagerElement<PoolResource<RA, RR, PI>> get(String index, Class<PI> classItem) {
        map.computeIfAbsent(index, _ -> classItem);
        return new ManagerElement<>(index, classItem, this);
    }

    @Override
    public PoolResource<?, ?, ?> build(String index, Class<?> classItem) {
        PoolResource<RA, RR, PI> poolResource = new PoolResource<>(index, 0, map.get(index));
        poolResource.run();
        return poolResource;
    }

}
