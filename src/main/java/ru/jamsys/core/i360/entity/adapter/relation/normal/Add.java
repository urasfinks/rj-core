package ru.jamsys.core.i360.entity.adapter.relation.normal;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.adapter.relation.Relation;

import java.util.List;

public class Add implements Relation {

    @Override
    public EntityChain compute(EntityChain leftEntityChain, EntityChain rightEntityChain) {
        List<Entity> leftEntityChainListEntity = leftEntityChain.getChain();
        List<Entity> rightEntityChainListEntity = rightEntityChain.getChain();
        EntityChain result = new EntityChain();
        List<Entity> listEntityResult = result.getChain();
        listEntityResult.addAll(leftEntityChainListEntity);
        listEntityResult.addAll(rightEntityChainListEntity);
        return listEntityResult.isEmpty() ? null : result;
    }

}
