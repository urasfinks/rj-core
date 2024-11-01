package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.Entity;

import java.util.List;
import java.util.Map;

public interface ScopeMapContext extends Scope {

    @Override
    default Context getContextByUuid(List<String> listUuid) {
        Context context = new Context();
        List<Entity> listEntity = context.getListEntity();
        Map<String, Entity> entityRepository = getMapEntity();
        listUuid.forEach(s -> listEntity.add(entityRepository.get(s)));
        return getMapContext().computeIfAbsent(context, _ -> context);
    }

    default boolean containsContext(Context context) {
        return getMapContext().containsKey(context);
    }

}
