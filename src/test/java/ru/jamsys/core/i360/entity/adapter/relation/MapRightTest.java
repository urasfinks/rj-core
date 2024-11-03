package ru.jamsys.core.i360.entity.adapter.relation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.EntityImpl;
import ru.jamsys.core.i360.entity.adapter.relation.normal.MapRight;

class MapRightTest {

    @Test
    public void test() {
        // Заменить ["1", "!", "2", "-"] mask ["2", "1", "2", "3"]  => ["-", "!", "-", "3"]

        EntityChain input = new EntityChain();
        input.getChain().add(new EntityImpl(null, "1"));
        input.getChain().add(new EntityImpl(null, "!"));
        input.getChain().add(new EntityImpl(null, "2"));
        input.getChain().add(new EntityImpl(null, "-"));

        EntityChain selection = new EntityChain();
        selection.getChain().add(new EntityImpl(null, "2"));
        selection.getChain().add(new EntityImpl(null, "1"));
        selection.getChain().add(new EntityImpl(null, "2"));
        selection.getChain().add(new EntityImpl(null, "3"));

        EntityChain result = new EntityChain();
        result.getChain().add(new EntityImpl(null, "-"));
        result.getChain().add(new EntityImpl(null, "!"));
        result.getChain().add(new EntityImpl(null, "-"));
        result.getChain().add(new EntityImpl(null, "3"));

        Assertions.assertEquals(result, new MapRight().compute(input, selection));

    }

}