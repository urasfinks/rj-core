package ru.jamsys.core.i360.entity;

// Единица информации (объект)
public interface Entity {

    String getUuid();

    Entity newInstance(String json) throws Throwable;

    static Entity newInstance(String json, Class<? extends Entity> cls) throws Throwable {
        return cls.getConstructor().newInstance().newInstance(json);
    }

}
