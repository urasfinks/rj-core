package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.Entity;

import java.util.List;
import java.util.Map;

public interface ScopeMapContext extends Scope {

    @Override
    default EntityChain getContextByUuid(List<String> listUuid) {
        EntityChain entityChain = new EntityChain();
        List<Entity> listEntity = entityChain.getListEntity();
        Map<String, Entity> entityRepository = getMapEntity();
        listUuid.forEach(s -> listEntity.add(entityRepository.get(s)));
        return getMapContext().computeIfAbsent(entityChain, _ -> entityChain);
    }

    default boolean containsContext(EntityChain entityChain) {
        return getMapContext().containsKey(entityChain);
    }

}
