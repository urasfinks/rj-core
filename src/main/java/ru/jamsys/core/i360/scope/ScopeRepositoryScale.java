package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.Scale;

import java.util.ArrayList;
import java.util.List;

public interface ScopeRepositoryScale extends Scope {

    default List<Scale> getListScale(EntityChain entityChain) {
        List<Scale> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getLeft().equals(entityChain) || scale.getRight().equals(entityChain)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<Scale> getListScaleByLeft(EntityChain entityChain) {
        List<Scale> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getLeft().equals(entityChain)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<Scale> getListScaleByRight(EntityChain entityChain) {
        List<Scale> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getRight().equals(entityChain)) {
                result.add(scale);
            }
        });
        return result;
    }

}
