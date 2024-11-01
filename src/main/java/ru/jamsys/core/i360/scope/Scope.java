package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.Entity;

import java.util.Map;

// Область знаний, состоит из
// 1) Сущности
// 2) Весы
// 3) Цепочки сущностей

public interface Scope {

    Map<String, Entity> getMapEntity();

    default ScopeRepositoryScale getRepositoryScale() {
        return (ScopeRepositoryScale) this;
    }

    default ScopeJsonDeserialize getJsonDeserialize() {
        return (ScopeJsonDeserialize) this;
    }

    default ScopeJsonSerialize getJsonSerialize() {
        return (ScopeJsonSerialize) this;
    }

    default ScopeRepositoryEntityChain getRepositoryEntityChain() {
        return (ScopeRepositoryEntityChain) this;
    }

}
