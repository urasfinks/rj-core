package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.scale.Scale;

import java.util.List;
import java.util.Map;

public interface Scope {

    Map<String, Entity> getEntityRepository();

    List<Scale> getListScale();

    Map<Context, Context> getContextRepository();

    Context getContext(List<String> listUuid);

    String toJson();

    void fromJson(String json) throws Throwable;

    void read(String path) throws Throwable;

    void write(String path) throws Throwable;

}
