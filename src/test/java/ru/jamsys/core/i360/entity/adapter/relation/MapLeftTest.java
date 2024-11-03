package ru.jamsys.core.i360.entity.adapter.relation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.EntityImpl;
import ru.jamsys.core.i360.entity.adapter.relation.reverse.MapLeft;

class MapLeftTest {

    @Test
    public void test() {
        // Заменить ["2", "1", "2", "3"] mask ["1", "2"]/["!", "-"] => ["-", "!", "-", "3"]
        EntityChain input = new EntityChain();
        input.getChain().add(new EntityImpl(null, "2"));
        input.getChain().add(new EntityImpl(null, "1"));
        input.getChain().add(new EntityImpl(null, "2"));
        input.getChain().add(new EntityImpl(null, "3"));

        EntityChain selection = new EntityChain();
        selection.getChain().add(new EntityImpl(null, "1"));
        selection.getChain().add(new EntityImpl(null, "!"));
        selection.getChain().add(new EntityImpl(null, "2"));
        selection.getChain().add(new EntityImpl(null, "-"));

        EntityChain result = new EntityChain();
        result.getChain().add(new EntityImpl(null, "-"));
        result.getChain().add(new EntityImpl(null, "!"));
        result.getChain().add(new EntityImpl(null, "-"));
        result.getChain().add(new EntityImpl(null, "3"));

        Assertions.assertEquals(result, new MapLeft().compute(input, selection));

    }

}