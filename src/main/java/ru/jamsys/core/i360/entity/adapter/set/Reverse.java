package ru.jamsys.core.i360.entity.adapter.set;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.Entity;

import java.util.List;

public class Reverse implements SetOperator {

    @Override
    public EntityChain transform(EntityChain entityChain, EntityChain entityChainSelection) {
        List<Entity> contextEntity = entityChain.getListEntity();
        EntityChain result = new EntityChain();
        List<Entity> listEntityResult = result.getListEntity();
        for (int i = contextEntity.size() - 1; i >= 0; i--) {
            listEntityResult.add(contextEntity.get(i));
        }
        return listEntityResult.isEmpty() ? null : result;
    }

}
