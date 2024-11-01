package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityChain;

import java.util.List;
import java.util.Map;

public interface ScopeRepositoryEntityChain extends Scope {

    Map<EntityChain, EntityChain> getMapEntityChain();

    default EntityChain getByUuids(List<String> listUuid) {
        EntityChain entityChain = new EntityChain();
        List<Entity> listEntity = entityChain.getListEntity();
        Map<String, Entity> mapEntity = getMapEntity();
        listUuid.forEach(uuid -> {
            if (mapEntity.containsKey(uuid)) {
                listEntity.add(mapEntity.get(uuid));
            }
        });
        return getMapEntityChain().computeIfAbsent(entityChain, _ -> entityChain);
    }

    default boolean contains(EntityChain entityChain) {
        return getMapEntityChain().containsKey(entityChain);
    }

}
