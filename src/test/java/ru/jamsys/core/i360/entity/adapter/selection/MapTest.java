package ru.jamsys.core.i360.entity.adapter.selection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.EntityImpl;

class MapTest {

    @Test
    public void test() {
        // Заменить ["2", "1", "2", "3"] mask ["1", "2"]/["!", "-"] => ["-", "!", "-", "3"]
        Context input = new Context();
        input.getListEntity().add(new EntityImpl(null, "2"));
        input.getListEntity().add(new EntityImpl(null, "1"));
        input.getListEntity().add(new EntityImpl(null, "2"));
        input.getListEntity().add(new EntityImpl(null, "3"));

        Context selection = new Context();
        selection.getListEntity().add(new EntityImpl(null, "1"));
        selection.getListEntity().add(new EntityImpl(null, "!"));
        selection.getListEntity().add(new EntityImpl(null, "2"));
        selection.getListEntity().add(new EntityImpl(null, "-"));

        Context result = new Context();
        result.getListEntity().add(new EntityImpl(null, "-"));
        result.getListEntity().add(new EntityImpl(null, "!"));
        result.getListEntity().add(new EntityImpl(null, "-"));
        result.getListEntity().add(new EntityImpl(null, "3"));

        Assertions.assertEquals(result, new Map().transform(input, selection));

    }

}