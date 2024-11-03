package ru.jamsys.core.i360.scale;

import ru.jamsys.core.i360.entity.EntityChain;

public interface Scale {

    EntityChain getLeft();

    EntityChain getRight();

    ScaleType getType();

    double getStability();

}
