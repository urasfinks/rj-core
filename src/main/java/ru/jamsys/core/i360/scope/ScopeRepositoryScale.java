package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.ScaleImpl;
import ru.jamsys.core.i360.scale.ScaleTypeRelation;

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

    default List<ScaleImpl> getByLeft(EntityChain entityChain, ScaleTypeRelation type) {
        List<ScaleImpl> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getLeft().equals(entityChain) && scale.getTypeRelation().equals(type)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<ScaleImpl> getByRight(EntityChain entityChain, ScaleTypeRelation type) {
        List<ScaleImpl> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getRight().equals(entityChain) && scale.getTypeRelation().equals(type)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<ScaleImpl> getByType(ScaleTypeRelation type) {
        List<ScaleImpl> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (type.equals(scale.getTypeRelation())) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<ScaleImpl> getByTypes(List<ScaleTypeRelation> types) {
        List<ScaleImpl> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (types.contains(scale.getTypeRelation())) {
                result.add(scale);
            }
        });
        return result;
    }

}
