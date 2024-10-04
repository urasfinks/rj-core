package ru.jamsys.core.i360;

import ru.jamsys.core.i360.entity.Entity;

import java.util.List;

public interface Scope {

    void load(String path) throws Throwable;

    void save(String path) throws Throwable;

    List<Entity> getListEntity();

    List<Scale> getListScale();

    String toJson();

    void fromJson(String json) throws Throwable;

    default Entity getEntityByUuid(String uuid) {
        for (Entity entity : getListEntity()) {
            if (entity.getUuid().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

}
