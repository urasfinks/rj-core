package ru.jamsys.core.i360;

import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.EntityImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EntityChainTest {

    @Test
    public void testHashCode(){
        EntityChain ctx1 = new EntityChain();
        ctx1.getListEntity().add(new EntityImpl("x1", "Hello"));
        ctx1.getListEntity().add(new EntityImpl("x2", "world"));

        EntityChain ctx2 = new EntityChain();
        ctx2.getListEntity().add(new EntityImpl("x1", "Hello"));
        ctx2.getListEntity().add(new EntityImpl("x2", "world"));

        assertEquals(ctx1, ctx2);

        ctx2.getListEntity().add(new EntityImpl("x3", "!"));

        assertNotEquals(ctx1, ctx2);

    }

}