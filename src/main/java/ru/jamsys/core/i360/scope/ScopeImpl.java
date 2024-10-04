package ru.jamsys.core.i360.scope;

import lombok.Getter;
import ru.jamsys.core.i360.scale.Scale;
import ru.jamsys.core.i360.entity.Entity;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ScopeImpl implements ScopeLoader {

    final private List<Entity> listEntity = new ArrayList<>();

    final private List<Scale> listScale = new ArrayList<>();

}
