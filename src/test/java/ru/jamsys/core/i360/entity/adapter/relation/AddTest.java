package ru.jamsys.core.i360.entity.adapter.relation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.EntityImpl;
import ru.jamsys.core.i360.entity.adapter.relation.normal.Add;

class AddTest {

    @Test
    public void test() {
        // Добавить ["1", "2"] mask ["3"] => ["1", "2", "3"]
        EntityChain input = new EntityChain();
        input.getChain().add(new EntityImpl(null, "1"));
        input.getChain().add(new EntityImpl(null, "2"));

        EntityChain selection = new EntityChain();
        selection.getChain().add(new EntityImpl(null, "3"));

        EntityChain result = new EntityChain();
        result.getChain().add(new EntityImpl(null, "1"));
        result.getChain().add(new EntityImpl(null, "2"));
        result.getChain().add(new EntityImpl(null, "3"));

        Assertions.assertEquals(result, new Add().compute(input, selection));

    }

}