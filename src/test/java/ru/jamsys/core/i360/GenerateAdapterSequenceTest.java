package ru.jamsys.core.i360;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.EntityImpl;

import java.util.ArrayList;

class GenerateAdapterSequenceTest {

    @Test
    void generate() {
        EntityChain ctx0 = new EntityChain();
        ctx0.getListEntity().add(new EntityImpl(null, "9"));

        EntityChain ctx1 = new EntityChain();
        ctx1.getListEntity().add(new EntityImpl(null, "0"));
        ctx1.getListEntity().add(new EntityImpl(null, "0"));
        ctx1.getListEntity().add(new EntityImpl(null, "9"));

        EntityChain ctx2 = new EntityChain();
        ctx2.getListEntity().add(new EntityImpl(null, "0"));
        ctx2.getListEntity().add(new EntityImpl(null, "9"));

        EntityChain ctx3 = new EntityChain();
        ctx3.getListEntity().add(new EntityImpl(null, "0"));
        ctx3.getListEntity().add(new EntityImpl(null, "0"));
        ctx3.getListEntity().add(new EntityImpl(null, "0"));
        ctx3.getListEntity().add(new EntityImpl(null, "9"));

        EntityChain ctx4 = new EntityChain();
        ctx4.getListEntity().add(new EntityImpl(null, "0"));
        ctx4.getListEntity().add(new EntityImpl(null, "0"));
        ctx4.getListEntity().add(new EntityImpl(null, "0"));
        ctx4.getListEntity().add(new EntityImpl(null, "0"));
        ctx4.getListEntity().add(new EntityImpl(null, "9"));

        ArrayList<EntityChain> listEntityChain = new ArrayList<>();
        listEntityChain.add(ctx0);
        listEntityChain.add(ctx1);
        listEntityChain.add(ctx2);
        listEntityChain.add(ctx3);
        listEntityChain.add(ctx4);
        GenerateSequence.generate(listEntityChain);

    }

    @Test
    void removeContains() {
        ArrayList<Entity> l1 = new ArrayList<>();
        l1.add(new EntityImpl(null, "0"));
        l1.add(new EntityImpl(null, "0"));

        ArrayList<Entity> l2 = new ArrayList<>();
        l2.add(new EntityImpl(null, "0"));

        Assertions.assertEquals(2, GenerateSequence.removeContains(l1, l2));
        Assertions.assertNull(GenerateSequence.removeContains(l2, l1));

    }

    @Test
    void cartesian() {
        ArrayList<String> l1 = new ArrayList<>();
        l1.add("a");

        ArrayList<String> l2 = new ArrayList<>();
        l2.add("b");
        l2.add("c");
        Assertions.assertEquals("[[a, b], [a, c]]", Util.cartesian(ArrayList::new, l1, l2).toString());
        Assertions.assertEquals("[[b, b], [b, c], [c, b], [c, c]]", Util.cartesian(ArrayList::new, l2, l2).toString());

    }
}