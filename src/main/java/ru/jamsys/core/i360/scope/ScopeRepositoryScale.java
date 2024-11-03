package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.ScaleImpl;
import ru.jamsys.core.i360.scale.ScaleType;

import java.util.ArrayList;
import java.util.List;

// Репозиторий весов

public interface ScopeRepositoryScale extends Scope {

    List<ScaleImpl> getListScale();

    default List<ScaleImpl> get(EntityChain entityChain) {
        List<ScaleImpl> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getLeft().equals(entityChain) || scale.getRight().equals(entityChain)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<ScaleImpl> getByLeft(EntityChain entityChain, ScaleType type) {
        List<ScaleImpl> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getLeft().equals(entityChain) && scale.getType().equals(type)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<ScaleImpl> getByRight(EntityChain entityChain, ScaleType type) {
        List<ScaleImpl> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getRight().equals(entityChain) && scale.getType().equals(type)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<ScaleImpl> getByType(ScaleType type) {
        List<ScaleImpl> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (type.equals(scale.getType())) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<ScaleImpl> getByTypes(List<ScaleType> types) {
        List<ScaleImpl> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (types.contains(scale.getType())) {
                result.add(scale);
            }
        });
        return result;
    }

}
