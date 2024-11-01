package ru.jamsys.core.i360.entity.adapter.set;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.Entity;

import java.util.List;

public class ForwardLeft implements SetOperator {

    @Override
    public EntityChain transform(EntityChain entityChain, EntityChain entityChainSelection) {
        List<Entity> contextEntity = entityChain.getListEntity();
        List<Entity> contextSelectionEntity = entityChainSelection.getListEntity();
        EntityChain result = new EntityChain();
        List<Entity> listEntityResult = result.getListEntity();
        contextEntity.forEach(entity -> {
            if (contextSelectionEntity.contains(entity)) {
                listEntityResult.add(entity);
            }
        });
        return listEntityResult.isEmpty() ? null : result;
    }

}
