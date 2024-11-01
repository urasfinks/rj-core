package ru.jamsys.core.i360.scope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.jamsys.core.i360.entity.Entity;

import java.util.Map;

// Область знаний, состоит из
// 1) Сущности
// 2) Весы
// 3) Цепочки сущностей (используются как ссылки в весах)

public interface Scope {

    Map<String, Entity> getMapEntity();

    @JsonIgnore
    default ScopeRepositoryScale getRepositoryScale() {
        return (ScopeRepositoryScale) this;
    }

    @JsonIgnore
    default ScopeJsonDeserialize getJsonDeserialize() {
        return (ScopeJsonDeserialize) this;
    }

    @JsonIgnore
    default ScopeJsonSerialize getJsonSerialize() {
        return (ScopeJsonSerialize) this;
    }

    @JsonIgnore
    default ScopeRepositoryEntityChain getRepositoryEntityChain() {
        return (ScopeRepositoryEntityChain) this;
    }

}
