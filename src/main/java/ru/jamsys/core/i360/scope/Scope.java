package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.Scale;

import java.util.List;
import java.util.Map;

public interface Scope {

    Map<String, Entity> getMapEntity();

    List<Scale> getListScale();

    Map<EntityChain, EntityChain> getMapEntityChain();

    default ScopeRepositoryScale getRepositoryScale() {
        return (ScopeRepositoryScale) this;
    }

    default ScopeJsonDeserialize getJsonDeserialize() {
        return (ScopeJsonDeserialize) this;
    }

    default ScopeJsonSerialize getJsonSerialize() {
        return (ScopeJsonSerialize) this;
    }

    default ScopeRepositoryEntityChain getScopeRepositoryEntityChain() {
        return (ScopeRepositoryEntityChain) this;
    }

}
