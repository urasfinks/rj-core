package ru.jamsys.core.i360.entity;

// Единица информации (объект)
public interface Entity {

    String getUuid();

    Entity newInstance(String json) throws Throwable;

    static Entity newInstance(String json, String className) throws Throwable {
        @SuppressWarnings("unchecked")
        Class<? extends Entity> cls = (Class<? extends Entity>) Class.forName(className);
        return cls.getConstructor().newInstance().newInstance(json);
    }

}
