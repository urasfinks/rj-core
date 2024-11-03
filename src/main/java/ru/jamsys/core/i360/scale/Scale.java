package ru.jamsys.core.i360.scale;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.operation.GeneralizationOperation;

public interface Scale {

    EntityChain getLeft();

    EntityChain getRight();

    ScaleType getType();

    double getStability();

    @JsonIgnore
    default GeneralizationOperation getGeneralizationOperation() {
        return (GeneralizationOperation) this;
    }

}
