package ru.jamsys.core.i360.entity.adapter.set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.EntityImpl;

class ForwardLeftWhileTest {

    @Test
    public void test() {
        // Пробрасывать пока встречаются ["2", "1", "2", "3"] mask ["2", "3"] => ["2"]
        Context input = new Context();
        input.getListEntity().add(new EntityImpl(null, "2"));
        input.getListEntity().add(new EntityImpl(null, "1"));
        input.getListEntity().add(new EntityImpl(null, "2"));
        input.getListEntity().add(new EntityImpl(null, "3"));

        Context selection = new Context();
        selection.getListEntity().add(new EntityImpl(null, "2"));
        selection.getListEntity().add(new EntityImpl(null, "3"));

        Context result = new Context();
        result.getListEntity().add(new EntityImpl(null, "2"));

        Assertions.assertEquals(result, new ForwardLeftWhile().transform(input, selection));

    }

}