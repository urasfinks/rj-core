package ru.jamsys.core.i360.entity.adapter.selection;

import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.Entity;

import java.util.List;

public class ForwardRight implements ContextSelection {

    @Override
    public Context transform(Context context, Context contextSelection) {
        List<Entity> contextEntity = context.getListEntity();
        List<Entity> contextSelectionEntity = contextSelection.getListEntity();
        Context result = new Context();
        List<Entity> listEntityResult = result.getListEntity();
        contextSelectionEntity.forEach(entity -> {
            if (contextEntity.contains(entity)) {
                listEntityResult.add(entity);
            }
        });
        return listEntityResult.isEmpty() ? null : result;
    }

}
