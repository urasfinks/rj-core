package ru.jamsys.core.i360.entity.adapter.relation;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.Entity;

import java.util.List;

public class RemoveLeftWhile implements Relation {

    @Override
    public EntityChain relation(EntityChain leftEntityChain, EntityChain rightEntityChain) {
        List<Entity> contextEntity = leftEntityChain.getListEntity();
        List<Entity> contextSelectionEntity = rightEntityChain.getListEntity();
        EntityChain result = new EntityChain();
        List<Entity> listEntityResult = result.getListEntity();
        listEntityResult.addAll(contextSelectionEntity);
        Entity[] array = listEntityResult.toArray(new Entity[0]);
        for (Entity entity : array) {
            if (contextEntity.contains(entity)) {
                listEntityResult.removeFirst();
            } else {
                break;
            }
        }
        return listEntityResult.isEmpty() ? null : result;
    }

}
