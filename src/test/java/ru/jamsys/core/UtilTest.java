package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.HashMapBuilder;
import ru.jamsys.core.flat.util.ListSort;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    void camelToSnake() {
        Assertions.assertEquals("HELLO_WORLD", Util.camelToSnake("HelloWorld"));
        Assertions.assertEquals("HELLO_WORLD1", Util.camelToSnake("HelloWorld1"));
        Assertions.assertEquals("HELLOWORLD", Util.camelToSnake("Helloworld"));
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


    public static class X {

    }

    @SuppressWarnings("all")
    @Test
    void testSet() {
        Set<X> set = Util.getConcurrentHashSet();
        X x = new X();
        set.add(x);
        set.add(x);

        Assertions.assertEquals(1, set.size());
    }

    @Test
    void listReversed(){
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        Assertions.assertEquals("[3, 2, 1]", list.reversed().toString());
    }

    @Test
    void listSort() {
        List<String> list = new ArrayList<>();
        list.add("apple");
        list.add("google");
        list.add("android");
        List<String> sort = ListSort.sort(list);
        Assertions.assertEquals("[android, apple, google]", sort.toString());
    }

}