package ru.jamsys.core.i360.entity.adapter.relation.reverse;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.adapter.relation.Relation;

import java.util.HashMap;
import java.util.List;

public class MapLeft implements Relation {

    @Override
    public EntityChain compute(EntityChain leftEntityChain, EntityChain rightEntityChain) {
        List<Entity> leftEntityChainListEntity = leftEntityChain.getChain();
        List<Entity> rightEntityChainListEntity = rightEntityChain.getChain();
        java.util.Map<Entity, Entity> map = new HashMap<>();
        for (int i = 0; i < rightEntityChainListEntity.size(); i += 2) {
            map.put(rightEntityChainListEntity.get(i), rightEntityChainListEntity.get(i + 1));
        }
        EntityChain result = new EntityChain();
        List<Entity> listEntityResult = result.getChain();
        leftEntityChainListEntity.forEach(entity -> listEntityResult.add(map.getOrDefault(entity, entity)));
        return listEntityResult.isEmpty() ? null : result;
    }

}
