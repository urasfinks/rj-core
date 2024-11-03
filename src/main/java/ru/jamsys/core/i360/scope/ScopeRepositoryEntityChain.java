package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityChain;

import java.util.List;
import java.util.Map;

// Репозиторий цепочек (весы ссылаются на цепочки)

public interface ScopeRepositoryEntityChain extends Scope {

    Map<EntityChain, EntityChain> getMapEntityChain();

    default EntityChain getByUuids(List<String> listUuid) {
        EntityChain entityChain = new EntityChain();
        List<Entity> listEntity = entityChain.getChain();
        Map<String, Entity> mapEntity = getMapEntity();
        listUuid.forEach(uuid -> {
            if (mapEntity.containsKey(uuid)) {
                listEntity.add(mapEntity.get(uuid));
            } else {
                throw new RuntimeException("MapEntity not exist: " + uuid);
            }
        });
        return getMapEntityChain().computeIfAbsent(entityChain, _ -> entityChain);
    }

    default boolean contains(EntityChain entityChain) {
        return getMapEntityChain().containsKey(entityChain);
    }

}
