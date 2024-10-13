package ru.jamsys.core.i360.entity.adapter.set;

import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.Entity;

import java.util.HashMap;
import java.util.List;

public class MapRight implements SetOperator {

    @Override
    public Context transform(Context context, Context contextSelection) {
        List<Entity> contextEntity = context.getListEntity();
        java.util.Map<Entity, Entity> map = new HashMap<>();
        for (int i = 0; i < contextEntity.size(); i += 2) {
            map.put(contextEntity.get(i), contextEntity.get(i + 1));
        }
        Context result = new Context();
        List<Entity> listEntityResult = result.getListEntity();
        contextSelection.getListEntity().forEach(entity -> listEntityResult.add(map.getOrDefault(entity, entity)));
        return listEntityResult.isEmpty() ? null : result;
    }

}
