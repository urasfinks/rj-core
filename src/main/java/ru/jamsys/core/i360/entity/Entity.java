package ru.jamsys.core.i360.entity;

import ru.jamsys.core.i360.scope.Scope;

import java.util.Map;

// Сущность - единица информации (объект)

public interface Entity {

    String path = Entity.class.getName().substring(0, Entity.class.getName().length() - Entity.class.getSimpleName().length());

    String getUuid();

    static Entity newInstance(Map<String, Object> map, Scope scope) throws Throwable {
        String className = (String) map.getOrDefault("class", EntityImpl.class.getSimpleName());
        @SuppressWarnings("unchecked")
        Class<? extends Entity> cls = (Class<? extends Entity>) Class.forName(path + className);
        return cls.getConstructor(Map.class, Scope.class).newInstance(map, scope);
    }

}
