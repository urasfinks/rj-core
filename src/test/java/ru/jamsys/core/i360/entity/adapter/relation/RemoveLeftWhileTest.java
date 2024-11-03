package ru.jamsys.core.i360.entity.adapter.relation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.EntityImpl;
import ru.jamsys.core.i360.entity.adapter.relation.reverse.RemoveLeftWhile;

class RemoveLeftWhileTest {

    @Test
    public void test() {
        // Удалять пока встречаются ["0", "0", "1", "2", "0"] mask ["0"] => ["1", "2", "0"]
        EntityChain input = new EntityChain();
        input.getChain().add(new EntityImpl(null, "0"));

        EntityChain selection = new EntityChain();
        selection.getChain().add(new EntityImpl(null, "0"));
        selection.getChain().add(new EntityImpl(null, "0"));
        selection.getChain().add(new EntityImpl(null, "1"));
        selection.getChain().add(new EntityImpl(null, "2"));
        selection.getChain().add(new EntityImpl(null, "0"));

        EntityChain result = new EntityChain();
        result.getChain().add(new EntityImpl(null, "1"));
        result.getChain().add(new EntityImpl(null, "2"));
        result.getChain().add(new EntityImpl(null, "0"));

        Assertions.assertEquals(result, new RemoveLeftWhile().compute(input, selection));

    }

}