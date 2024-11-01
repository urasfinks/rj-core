package ru.jamsys.core.i360.entity.adapter.relation;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.Entity;

import java.util.List;

public class Intersection implements Relation {

    @Override
    public EntityChain relation(EntityChain leftEntityChain, EntityChain rightEntityChain) {
        List<Entity> contextEntity = leftEntityChain.getListEntity();
        List<Entity> contextSelectionEntity = rightEntityChain.getListEntity();
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
