package ru.jamsys.core.i360.entity.adapter.selection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.EntityImpl;

class ReverseTest {

    @Test
    public void test() {
        // Задом на перёд, для решения задач удаления с конца всех нулей например
        Context input = new Context();
        input.getListEntity().add(new EntityImpl(null, "1"));
        input.getListEntity().add(new EntityImpl(null, "2"));
        input.getListEntity().add(new EntityImpl(null, "3"));


        Context result = new Context();
        result.getListEntity().add(new EntityImpl(null, "3"));
        result.getListEntity().add(new EntityImpl(null, "2"));
        result.getListEntity().add(new EntityImpl(null, "1"));

        Assertions.assertEquals(result, new Reverse().transform(input, null));

    }

}