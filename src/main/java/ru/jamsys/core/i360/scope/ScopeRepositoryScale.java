package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.Scale;
import ru.jamsys.core.i360.scale.ScaleType;

import java.util.ArrayList;
import java.util.List;

// Репозиторий весов

public interface ScopeRepositoryScale extends Scope {

    List<Scale> getListScale();

    default List<Scale> get(EntityChain entityChain) {
        List<Scale> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getLeft().equals(entityChain) || scale.getRight().equals(entityChain)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<Scale> getByLeft(EntityChain entityChain) {
        List<Scale> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getLeft().equals(entityChain)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<Scale> getByRight(EntityChain entityChain) {
        List<Scale> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getRight().equals(entityChain)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<Scale> getByType(ScaleType type) {
        List<Scale> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getType().equals(type)) {
                result.add(scale);
            }
        });
        return result;
    }

}
