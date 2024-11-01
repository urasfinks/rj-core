package ru.jamsys.core.i360.entity.adapter.set;

import ru.jamsys.core.i360.entity.EntityChain;

public interface SetOperator {

    EntityChain transform(EntityChain entityChain, EntityChain entityChainSelection);

}
