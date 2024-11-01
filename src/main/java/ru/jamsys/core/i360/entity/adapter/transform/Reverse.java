package ru.jamsys.core.i360.entity.adapter.transform;

import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityChain;

import java.util.List;

public class Reverse implements Transform {

    @Override
    public EntityChain transform(EntityChain leftEntityChain) {
        List<Entity> contextEntity = leftEntityChain.getListEntity();
        EntityChain result = new EntityChain();
        List<Entity> listEntityResult = result.getListEntity();
        for (int i = contextEntity.size() - 1; i >= 0; i--) {
            listEntityResult.add(contextEntity.get(i));
        }
        return listEntityResult.isEmpty() ? null : result;
    }

}
