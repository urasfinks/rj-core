package ru.jamsys.core.i360.entity.adapter.set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.EntityImpl;

class MapRightTest {

    @Test
    public void test() {
        // Заменить ["1", "!", "2", "-"] mask ["2", "1", "2", "3"]  => ["-", "!", "-", "3"]

        EntityChain input = new EntityChain();
        input.getListEntity().add(new EntityImpl(null, "1"));
        input.getListEntity().add(new EntityImpl(null, "!"));
        input.getListEntity().add(new EntityImpl(null, "2"));
        input.getListEntity().add(new EntityImpl(null, "-"));

        EntityChain selection = new EntityChain();
        selection.getListEntity().add(new EntityImpl(null, "2"));
        selection.getListEntity().add(new EntityImpl(null, "1"));
        selection.getListEntity().add(new EntityImpl(null, "2"));
        selection.getListEntity().add(new EntityImpl(null, "3"));

        EntityChain result = new EntityChain();
        result.getListEntity().add(new EntityImpl(null, "-"));
        result.getListEntity().add(new EntityImpl(null, "!"));
        result.getListEntity().add(new EntityImpl(null, "-"));
        result.getListEntity().add(new EntityImpl(null, "3"));

        Assertions.assertEquals(result, new MapRight().transform(input, selection));

    }

}