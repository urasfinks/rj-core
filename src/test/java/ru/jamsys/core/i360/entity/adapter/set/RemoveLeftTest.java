package ru.jamsys.core.i360.entity.adapter.set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.EntityImpl;

class RemoveLeftTest {

    @Test
    public void test() {
        // ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["4", "5"]
        EntityChain input = new EntityChain();
        input.getListEntity().add(new EntityImpl(null, "0"));
        input.getListEntity().add(new EntityImpl(null, "1"));
        input.getListEntity().add(new EntityImpl(null, "2"));
        input.getListEntity().add(new EntityImpl(null, "3"));

        EntityChain selection = new EntityChain();
        selection.getListEntity().add(new EntityImpl(null, "2"));
        selection.getListEntity().add(new EntityImpl(null, "2"));
        selection.getListEntity().add(new EntityImpl(null, "3"));
        selection.getListEntity().add(new EntityImpl(null, "4"));
        selection.getListEntity().add(new EntityImpl(null, "5"));

        EntityChain result = new EntityChain();
        result.getListEntity().add(new EntityImpl(null, "4"));
        result.getListEntity().add(new EntityImpl(null, "5"));

        Assertions.assertEquals(result, new RemoveLeft().transform(input, selection));

    }

}