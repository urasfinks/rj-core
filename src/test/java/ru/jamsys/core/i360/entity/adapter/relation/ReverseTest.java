package ru.jamsys.core.i360.entity.adapter.relation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.EntityImpl;
import ru.jamsys.core.i360.entity.adapter.transform.Reverse;

class ReverseTest {

    @Test
    public void test() {
        // Задом на перёд, для решения задач удаления с конца всех нулей например
        EntityChain input = new EntityChain();
        input.getChain().add(new EntityImpl(null, "1"));
        input.getChain().add(new EntityImpl(null, "2"));
        input.getChain().add(new EntityImpl(null, "3"));


        EntityChain result = new EntityChain();
        result.getChain().add(new EntityImpl(null, "3"));
        result.getChain().add(new EntityImpl(null, "2"));
        result.getChain().add(new EntityImpl(null, "1"));

        Assertions.assertEquals(result, new Reverse().transform(input));

    }

}