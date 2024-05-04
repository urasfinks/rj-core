package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.util.Util;

import java.util.ArrayList;
import java.util.List;

class UtilTest {

    @Test
    void snakeToCamel() {
        Assertions.assertEquals("HelloWorld", Util.snakeToCamel("HELLO_WORLD"));
        Assertions.assertEquals("HelloWorld1", Util.snakeToCamel("HELLO_WORLD1"));
        Assertions.assertEquals("HelloWorld1", Util.snakeToCamel("HELLO_WORLD_1"));
        Assertions.assertEquals("Helloworld", Util.snakeToCamel("HELLOWORLD"));
    }

    @Test
    void testRiscCollection() {
        List<String> list = new ArrayList<>();

        list.add("Hello");
        list.add("world");
        list.add("!");

        List<String> result = new ArrayList<>();
        Util.riskModifierCollection(null, list, new String[0], result::add);
        Assertions.assertEquals("[Hello, world, !]", result.toString());

        List<String> result2 = new ArrayList<>();
        Util.riskModifierCollection(null, list, new String[0], result2::add, true);
        Assertions.assertEquals("[!, world, Hello]", result2.toString());

    }
}