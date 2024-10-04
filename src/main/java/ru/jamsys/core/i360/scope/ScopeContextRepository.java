package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.Entity;

import java.util.List;
import java.util.Map;

public interface ScopeContextRepository extends Scope {

    @Override
    default Context getContext(List<String> listUuid) {
        Context context = new Context();
        List<Entity> listEntity = context.getListEntity();
        Map<String, Entity> entityRepository = getEntityRepository();
        listUuid.forEach(s -> listEntity.add(entityRepository.get(s)));
        return getContextRepository().computeIfAbsent(context, _ -> context);
    }

}
