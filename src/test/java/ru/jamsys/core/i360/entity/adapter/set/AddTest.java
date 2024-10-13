package ru.jamsys.core.i360.entity.adapter.set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.EntityImpl;

class AddTest {

    @Test
    public void test() {
        // Добавить ["1", "2"] mask ["3"] => ["1", "2", "3"]
        Context input = new Context();
        input.getListEntity().add(new EntityImpl(null, "1"));
        input.getListEntity().add(new EntityImpl(null, "2"));

        Context selection = new Context();
        selection.getListEntity().add(new EntityImpl(null, "3"));

        Context result = new Context();
        result.getListEntity().add(new EntityImpl(null, "1"));
        result.getListEntity().add(new EntityImpl(null, "2"));
        result.getListEntity().add(new EntityImpl(null, "3"));

        Assertions.assertEquals(result, new Add().transform(input, selection));

    }

}