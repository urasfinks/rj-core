package ru.jamsys.core.i360.entity.adapter.relation.normal;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.adapter.relation.Relation;

import java.util.HashMap;
import java.util.List;

public class MapRight implements Relation {

    @Override
    public EntityChain compute(EntityChain leftEntityChain, EntityChain rightEntityChain) {
        List<Entity> leftEntityChainListEntity = leftEntityChain.getListEntity();
        java.util.Map<Entity, Entity> map = new HashMap<>();
        for (int i = 0; i < leftEntityChainListEntity.size(); i += 2) {
            map.put(leftEntityChainListEntity.get(i), leftEntityChainListEntity.get(i + 1));
        }
        EntityChain result = new EntityChain();
        List<Entity> listEntityResult = result.getListEntity();
        rightEntityChain.getListEntity().forEach(entity -> listEntityResult.add(map.getOrDefault(entity, entity)));
        return listEntityResult.isEmpty() ? null : result;
    }

}
