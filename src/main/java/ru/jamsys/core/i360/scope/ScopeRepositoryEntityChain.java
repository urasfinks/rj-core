package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityChain;

import java.util.List;
import java.util.Map;

public interface ScopeRepositoryEntityChain extends Scope {

    default EntityChain getByUuids(List<String> listUuid) {
        EntityChain entityChain = new EntityChain();
        List<Entity> listEntity = entityChain.getListEntity();
        Map<String, Entity> entityRepository = getMapEntity();
        listUuid.forEach(s -> {
            if (entityRepository.containsKey(s)) {
                listEntity.add(entityRepository.get(s));
            }
        });
        return getMapEntityChain().computeIfAbsent(entityChain, _ -> entityChain);
    }

    default boolean containsEntityChain(EntityChain entityChain) {
        return getMapEntityChain().containsKey(entityChain);
    }

}
