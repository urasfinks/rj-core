package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.HashMapBuilder;
import ru.jamsys.core.util.Util;
import ru.jamsys.core.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
        UtilRisc.forEach(null, list, (String s) -> {
            result.add(s);
        });
        Assertions.assertEquals("[Hello, world, !]", result.toString());

        List<String> result2 = new ArrayList<>();
        UtilRisc.forEach(null, list, (String s) -> {
            result2.add(s);
        }, true);
        Assertions.assertEquals("[!, world, Hello]", result2.toString());

    }

    @Test
    void testRiskCollection() {
        AtomicInteger counter = new AtomicInteger(0);
        StringBuilder sb = new StringBuilder();
        Map<String, Object> map = new HashMapBuilder<String, Object>()
                .append("test", 1)
                .append("x", "y");
        UtilRisc.forEach(null, map, (String key, Object value) -> {
            counter.incrementAndGet();
            sb.append(key);
            sb.append(value);
        });
        Assertions.assertEquals(2, counter.get());
        Assertions.assertEquals("test1xy", sb.toString());
    }
}