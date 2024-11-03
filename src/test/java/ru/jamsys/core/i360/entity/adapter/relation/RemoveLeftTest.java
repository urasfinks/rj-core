package ru.jamsys.core.i360.entity.adapter.relation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.EntityImpl;
import ru.jamsys.core.i360.entity.adapter.relation.reverse.RemoveLeft;

class RemoveLeftTest {

    @Test
    public void test() {
        // ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["4", "5"]
        EntityChain input = new EntityChain();
        input.getChain().add(new EntityImpl(null, "0"));
        input.getChain().add(new EntityImpl(null, "1"));
        input.getChain().add(new EntityImpl(null, "2"));
        input.getChain().add(new EntityImpl(null, "3"));

        EntityChain selection = new EntityChain();
        selection.getChain().add(new EntityImpl(null, "2"));
        selection.getChain().add(new EntityImpl(null, "2"));
        selection.getChain().add(new EntityImpl(null, "3"));
        selection.getChain().add(new EntityImpl(null, "4"));
        selection.getChain().add(new EntityImpl(null, "5"));

        EntityChain result = new EntityChain();
        result.getChain().add(new EntityImpl(null, "4"));
        result.getChain().add(new EntityImpl(null, "5"));

        Assertions.assertEquals(result, new RemoveLeft().compute(input, selection));

    }

}