package ru.jamsys.core.i360.scope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.ScaleImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ScopeImpl implements
        Scope,
        ScopeRepositoryEntityChain,
        ScopeRepositoryScale,
        ScopeJsonDeserialize,
        ScopeJsonSerialize {

    public ScopeImpl() {
    }

    public ScopeImpl(String path) throws Throwable {
        getJsonDeserialize().read(path);
    }

    // key: uuid; value: Entity
    final private Map<String, Entity> mapEntity = new HashMap<>();

    // Список весов
    final private List<ScaleImpl> listScale = new ArrayList<>();

    // Все контексты уникальны в пределах Scope
    @JsonIgnore
    final private Map<EntityChain, EntityChain> mapEntityChain = new HashMap<>();

}
