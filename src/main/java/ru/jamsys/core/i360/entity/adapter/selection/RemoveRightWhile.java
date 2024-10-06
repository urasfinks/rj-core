package ru.jamsys.core.i360.entity.adapter.selection;

import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.Entity;

import java.util.List;

public class RemoveRightWhile implements ContextSelection {

    @Override
    public Context transform(Context context, Context contextSelection) {
        List<Entity> contextEntity = context.getListEntity();
        List<Entity> contextSelectionEntity = contextSelection.getListEntity();
        Context result = new Context();
        List<Entity> listEntityResult = result.getListEntity();
        listEntityResult.addAll(contextEntity);
        Entity[] array = listEntityResult.toArray(new Entity[0]);
        for (Entity entity : array) {
            if (contextSelectionEntity.contains(entity)) {
                listEntityResult.removeFirst();
            } else {
                break;
            }
        }
        return listEntityResult.isEmpty() ? null : result;
    }

}
