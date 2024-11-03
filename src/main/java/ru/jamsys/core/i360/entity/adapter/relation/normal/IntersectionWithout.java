package ru.jamsys.core.i360.entity.adapter.relation.normal;

import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.adapter.relation.Relation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Оба множества без пересечения ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["0", "1", "4","5"]
public class IntersectionWithout implements Relation {

    @Override
    public EntityChain compute(EntityChain leftEntityChain, EntityChain rightEntityChain) {
        List<Entity> leftEntityChainListEntity = leftEntityChain.getChain();
        List<Entity> rightEntityChainListEntity = rightEntityChain.getChain();
        EntityChain result = new EntityChain();
        List<Entity> listEntityResult = result.getChain();
        Set<Entity> s = new HashSet<>();
        s.addAll(leftEntityChainListEntity);
        s.addAll(rightEntityChainListEntity);
        leftEntityChainListEntity.forEach(entity -> {
            if (rightEntityChainListEntity.contains(entity)) {
                s.remove(entity);
            }
        });
        listEntityResult.addAll(s);
        return listEntityResult.isEmpty() ? null : result;
    }

}
