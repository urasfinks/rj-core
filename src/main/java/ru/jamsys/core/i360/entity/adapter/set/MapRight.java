package ru.jamsys.core.i360.entity.adapter.set;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.Entity;

import java.util.HashMap;
import java.util.List;

public class MapRight implements SetOperator {

    @Override
    public EntityChain transform(EntityChain entityChain, EntityChain entityChainSelection) {
        List<Entity> contextEntity = entityChain.getListEntity();
        java.util.Map<Entity, Entity> map = new HashMap<>();
        for (int i = 0; i < contextEntity.size(); i += 2) {
            map.put(contextEntity.get(i), contextEntity.get(i + 1));
        }
        EntityChain result = new EntityChain();
        List<Entity> listEntityResult = result.getListEntity();
        entityChainSelection.getListEntity().forEach(entity -> listEntityResult.add(map.getOrDefault(entity, entity)));
        return listEntityResult.isEmpty() ? null : result;
    }

}
