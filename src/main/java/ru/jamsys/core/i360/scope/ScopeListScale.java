package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.Scale;

import java.util.ArrayList;
import java.util.List;

public interface ScopeListScale extends Scope {

    default List<Scale> getScaleByLeft(EntityChain entityChain) {
        List<Scale> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getLeft().equals(entityChain)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<Scale> getScaleByRight(EntityChain entityChain) {
        List<Scale> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getRight().equals(entityChain)) {
                result.add(scale);
            }
        });
        return result;
    }

}
