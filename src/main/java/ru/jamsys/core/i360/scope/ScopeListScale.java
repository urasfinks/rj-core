package ru.jamsys.core.i360.scope;

import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.scale.Scale;

import java.util.ArrayList;
import java.util.List;

public interface ScopeListScale extends Scope {

    default List<Scale> getScaleByLeft(Context context) {
        List<Scale> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getLeft().equals(context)) {
                result.add(scale);
            }
        });
        return result;
    }

    default List<Scale> getScaleByRight(Context context) {
        List<Scale> result = new ArrayList<>();
        getListScale().forEach(scale -> {
            if (scale.getRight().equals(context)) {
                result.add(scale);
            }
        });
        return result;
    }

}
