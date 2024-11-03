package ru.jamsys.core.i360.entity.adapter.relation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.EntityImpl;
import ru.jamsys.core.i360.entity.adapter.relation.normal.RemoveRight;

class RemoveRightTest {

    @Test
    public void preTest() {
        EntityImpl entity1 = new EntityImpl(null, "0");
        EntityImpl entity2 = new EntityImpl(null, "0");
        Assertions.assertEquals(entity1, entity2);
    }

    @Test
    public void test() {
        // Удалить из всего контекста ["0", "0", "1", "2", "0"] mask ["0"] => ["1", "2"]
        EntityChain input = new EntityChain();
        input.getChain().add(new EntityImpl(null, "0"));
        input.getChain().add(new EntityImpl(null, "0"));
        input.getChain().add(new EntityImpl(null, "1"));
        input.getChain().add(new EntityImpl(null, "2"));
        input.getChain().add(new EntityImpl(null, "0"));

        EntityChain selection = new EntityChain();
        selection.getChain().add(new EntityImpl(null, "0"));

        EntityChain result = new EntityChain();
        result.getChain().add(new EntityImpl(null, "1"));
        result.getChain().add(new EntityImpl(null, "2"));

        Assertions.assertEquals(result, new RemoveRight().compute(input, selection));

    }

    @Test
    public void test2() {
        // ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["0", "1"]
        EntityChain input = new EntityChain();
        input.getChain().add(new EntityImpl(null, "0"));
        input.getChain().add(new EntityImpl(null, "1"));
        input.getChain().add(new EntityImpl(null, "2"));
        input.getChain().add(new EntityImpl(null, "3"));

        EntityChain selection = new EntityChain();
        selection.getChain().add(new EntityImpl(null, "2"));
        selection.getChain().add(new EntityImpl(null, "3"));
        selection.getChain().add(new EntityImpl(null, "4"));
        selection.getChain().add(new EntityImpl(null, "5"));

        EntityChain result = new EntityChain();
        result.getChain().add(new EntityImpl(null, "0"));
        result.getChain().add(new EntityImpl(null, "1"));

        Assertions.assertEquals(result, new RemoveRight().compute(input, selection));

    }

}