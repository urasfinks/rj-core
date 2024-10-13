package ru.jamsys.core.i360.entity.adapter.set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.EntityImpl;

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
        Context input = new Context();
        input.getListEntity().add(new EntityImpl(null, "0"));
        input.getListEntity().add(new EntityImpl(null, "0"));
        input.getListEntity().add(new EntityImpl(null, "1"));
        input.getListEntity().add(new EntityImpl(null, "2"));
        input.getListEntity().add(new EntityImpl(null, "0"));

        Context selection = new Context();
        selection.getListEntity().add(new EntityImpl(null, "0"));

        Context result = new Context();
        result.getListEntity().add(new EntityImpl(null, "1"));
        result.getListEntity().add(new EntityImpl(null, "2"));

        Assertions.assertEquals(result, new RemoveRight().transform(input, selection));

    }

    @Test
    public void test2() {
        // ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["0", "1"]
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
        result.getListEntity().add(new EntityImpl(null, "0"));
        result.getListEntity().add(new EntityImpl(null, "1"));

        Assertions.assertEquals(result, new RemoveRight().transform(input, selection));

    }

}