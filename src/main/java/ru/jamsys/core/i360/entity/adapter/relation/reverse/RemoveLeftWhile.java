package ru.jamsys.core.i360.entity.adapter.relation.reverse;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.adapter.relation.Relation;

import java.util.List;

public class RemoveLeftWhile implements Relation {

    @Override
    public EntityChain compute(EntityChain leftEntityChain, EntityChain rightEntityChain) {
        List<Entity> leftEntityChainListEntity = leftEntityChain.getListEntity();
        List<Entity> rightEntityChainListEntity = rightEntityChain.getListEntity();
        EntityChain result = new EntityChain();
        List<Entity> listEntityResult = result.getListEntity();
        listEntityResult.addAll(rightEntityChainListEntity);
        Entity[] array = listEntityResult.toArray(new Entity[0]);
        for (Entity entity : array) {
            if (leftEntityChainListEntity.contains(entity)) {
                listEntityResult.removeFirst();
            } else {
                break;
            }
        }
        return listEntityResult.isEmpty() ? null : result;
    }

}
