package ru.jamsys.core.i360.entity.adapter.selection;

import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.Entity;

import java.util.HashMap;
import java.util.List;

public class MapLeft implements ContextSelection {

    @Override
    public Context transform(Context context, Context contextSelection) {
        List<Entity> contextEntity = context.getListEntity();
        List<Entity> contextSelectionEntity = contextSelection.getListEntity();
        java.util.Map<Entity, Entity> map = new HashMap<>();
        for (int i = 0; i < contextSelectionEntity.size(); i += 2) {
            map.put(contextSelectionEntity.get(i), contextSelectionEntity.get(i + 1));
        }
        Context result = new Context();
        List<Entity> listEntityResult = result.getListEntity();
        contextEntity.forEach(entity -> listEntityResult.add(map.getOrDefault(entity, entity)));
        return listEntityResult.isEmpty() ? null : result;
    }

}
