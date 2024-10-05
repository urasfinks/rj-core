package ru.jamsys.core.i360.entity.adapter.selection;

import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.Entity;

import java.util.List;

public class Reverse implements ContextSelection {

    @Override
    public Context transform(Context context, Context contextSelection) {
        List<Entity> contextEntity = context.getListEntity();
        Context result = new Context();
        List<Entity> listEntityResult = result.getListEntity();
        for (int i = contextEntity.size() - 1; i >= 0; i--) {
            listEntityResult.add(contextEntity.get(i));
        }
        return listEntityResult.isEmpty() ? null : result;
    }

}
