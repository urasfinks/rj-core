package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.scale.Scale;

import java.util.List;
import java.util.Map;

public interface Scope {

    Map<String, Entity> getMapEntity();

    List<Scale> getListScale();

    Map<EntityChain, EntityChain> getMapEntityChain();

    EntityChain getEntityChainByUuids(List<String> listUuid);

    boolean containsEntityChain(EntityChain entityChain);

    String toJson();

    void fromJson(String json) throws Throwable;

    void read(String path) throws Throwable;

    void write(String path) throws Throwable;

    List<Scale> getScaleByLeft(EntityChain entityChain);

    List<Scale> getScaleByRight(EntityChain entityChain);

}
