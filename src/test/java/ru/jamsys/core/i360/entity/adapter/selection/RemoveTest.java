package ru.jamsys.core.i360.entity.adapter.selection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.EntityImpl;

class RemoveTest {

    @Test
    public void preTest() {
        EntityImpl entity1 = new EntityImpl(null, "0");
        EntityImpl entity2 = new EntityImpl(null, "0");
        Assertions.assertEquals(entity1, entity2);
    }

    @Test
    public void test() {
        Remove remove = new Remove();
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

        Assertions.assertEquals(result, remove.transform(input, selection));

    }

}