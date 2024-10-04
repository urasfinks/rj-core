package ru.jamsys.core.i360;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.EntityImpl;

import static org.junit.jupiter.api.Assertions.*;

class ContextTest {

    @Test
    public void testHashCode(){
        Context ctx1 = new Context();
        ctx1.getListEntity().add(new EntityImpl("x1", "Hello"));
        ctx1.getListEntity().add(new EntityImpl("x2", "world"));

        Context ctx2 = new Context();
        ctx2.getListEntity().add(new EntityImpl("x1", "Hello"));
        ctx2.getListEntity().add(new EntityImpl("x2", "world"));

        assertEquals(ctx1, ctx2);

        ctx2.getListEntity().add(new EntityImpl("x3", "!"));

        assertNotEquals(ctx1, ctx2);

    }

}