package ru.jamsys.core.i360.scale.operation;

import ru.jamsys.core.i360.scale.Scale;
import ru.jamsys.core.i360.scope.Scope;

public interface GeneralizationOperation extends Scale {

    // Получить дерево обобщений
    default GeneralizationTree get(Scope scope) {
        return new GeneralizationTree(scope, getLeft());
    }

}
