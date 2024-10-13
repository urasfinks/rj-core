package ru.jamsys.core.i360.entity.adapter.set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.EntityImpl;

class RemoveLeftWhileTest {

    @Test
    public void test() {
        // Удалять пока встречаются ["0", "0", "1", "2", "0"] mask ["0"] => ["1", "2", "0"]
        Context input = new Context();
        input.getListEntity().add(new EntityImpl(null, "0"));

        Context selection = new Context();
        selection.getListEntity().add(new EntityImpl(null, "0"));
        selection.getListEntity().add(new EntityImpl(null, "0"));
        selection.getListEntity().add(new EntityImpl(null, "1"));
        selection.getListEntity().add(new EntityImpl(null, "2"));
        selection.getListEntity().add(new EntityImpl(null, "0"));

        Context result = new Context();
        result.getListEntity().add(new EntityImpl(null, "1"));
        result.getListEntity().add(new EntityImpl(null, "2"));
        result.getListEntity().add(new EntityImpl(null, "0"));

        Assertions.assertEquals(result, new RemoveLeftWhile().transform(input, selection));

    }

}