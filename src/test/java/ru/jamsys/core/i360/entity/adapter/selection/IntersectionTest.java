package ru.jamsys.core.i360.entity.adapter.selection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.EntityImpl;

class IntersectionTest {

    @Test
    public void test() {
        // Пересечение множеств ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["2", "3"]
        Context input = new Context();
        input.getListEntity().add(new EntityImpl(null, "0"));
        input.getListEntity().add(new EntityImpl(null, "1"));
        input.getListEntity().add(new EntityImpl(null, "2"));
        input.getListEntity().add(new EntityImpl(null, "3"));

        Context selection = new Context();
        selection.getListEntity().add(new EntityImpl(null, "2"));
        selection.getListEntity().add(new EntityImpl(null, "3"));
        selection.getListEntity().add(new EntityImpl(null, "4"));
        selection.getListEntity().add(new EntityImpl(null, "5"));

        Context result = new Context();
        result.getListEntity().add(new EntityImpl(null, "2"));
        result.getListEntity().add(new EntityImpl(null, "3"));

        Assertions.assertEquals(result, new Intersection().transform(input, selection));

    }

}