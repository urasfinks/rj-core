package ru.jamsys.core.i360.scope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.scale.Scale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ScopeImpl implements Scope, ScopeJsonDeserialize, ScopeJsonSerialize, ScopeIO, ScopeContextRepository {

    final private Map<String, Entity> entityRepository = new HashMap<>();

    final private List<Scale> listScale = new ArrayList<>();

    @JsonIgnore
    final private Map<Context, Context> contextRepository = new HashMap<>();

}
