package ru.jamsys.core.i360;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityImpl;

import java.util.ArrayList;

class GenerateTemplateTest {

    @Test
    void generate() {
        Context ctx0 = new Context();
        ctx0.getListEntity().add(new EntityImpl(null, "9"));

        Context ctx1 = new Context();
        ctx1.getListEntity().add(new EntityImpl(null, "0"));
        ctx1.getListEntity().add(new EntityImpl(null, "0"));
        ctx1.getListEntity().add(new EntityImpl(null, "9"));

        Context ctx2 = new Context();
        ctx2.getListEntity().add(new EntityImpl(null, "0"));
        ctx2.getListEntity().add(new EntityImpl(null, "9"));

        Context ctx3 = new Context();
        ctx3.getListEntity().add(new EntityImpl(null, "0"));
        ctx3.getListEntity().add(new EntityImpl(null, "0"));
        ctx3.getListEntity().add(new EntityImpl(null, "0"));
        ctx3.getListEntity().add(new EntityImpl(null, "9"));

        Context ctx4 = new Context();
        ctx4.getListEntity().add(new EntityImpl(null, "0"));
        ctx4.getListEntity().add(new EntityImpl(null, "0"));
        ctx4.getListEntity().add(new EntityImpl(null, "0"));
        ctx4.getListEntity().add(new EntityImpl(null, "0"));
        ctx4.getListEntity().add(new EntityImpl(null, "9"));

        ArrayList<Context> listContext = new ArrayList<>();
        listContext.add(ctx0);
        listContext.add(ctx1);
        listContext.add(ctx2);
        listContext.add(ctx3);
        listContext.add(ctx4);
        GenerateTemplate.generate(listContext);

    }

    @Test
    void removeContains() {
        ArrayList<Entity> l1 = new ArrayList<>();
        l1.add(new EntityImpl(null, "0"));
        l1.add(new EntityImpl(null, "0"));

        ArrayList<Entity> l2 = new ArrayList<>();
        l2.add(new EntityImpl(null, "0"));

        Assertions.assertEquals(2, GenerateTemplate.removeContains(l1, l2));
        Assertions.assertNull(GenerateTemplate.removeContains(l2, l1));

    }

    @Test
    void cartesian() {
        ArrayList<String> l1 = new ArrayList<>();
        l1.add("a");

        ArrayList<String> l2 = new ArrayList<>();
        l2.add("b");
        l2.add("c");
        Assertions.assertEquals("[[a, b], [a, c]]", GenerateTemplate.cartesian(ArrayList::new, l1, l2).toString());
        Assertions.assertEquals("[[b, b], [b, c], [c, b], [c, c]]",GenerateTemplate.cartesian(ArrayList::new, l2, l2).toString());

    }
}