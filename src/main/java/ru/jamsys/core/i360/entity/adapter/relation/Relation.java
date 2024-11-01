package ru.jamsys.core.i360.entity.adapter.relation;

import ru.jamsys.core.i360.entity.EntityChain;

public interface Relation {

    EntityChain compute(EntityChain leftEntityChain, EntityChain rightEntityChain);

}
