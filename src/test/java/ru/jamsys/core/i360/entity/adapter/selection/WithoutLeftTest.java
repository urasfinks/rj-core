package ru.jamsys.core.i360.entity.adapter.selection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.EntityImpl;

class WithoutLeftTest {

    @Test
    public void test() {
        // ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["4", "5"]
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
        result.getListEntity().add(new EntityImpl(null, "4"));
        result.getListEntity().add(new EntityImpl(null, "5"));

        Assertions.assertEquals(result, new WithoutLeft().transform(input, selection));

    }

}