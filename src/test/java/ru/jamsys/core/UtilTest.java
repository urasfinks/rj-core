package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilListSort;
import ru.jamsys.core.flat.util.UtilRisc;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// IO time: 13ms
// COMPUTE time: 10ms

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

    @Test
    void anyTest() {
        Map<String, Object> source = new HashMap<>();
        source.put("z", 1);
        Map<String, Object> append = new HashMapBuilder<>(source)
                .append("x", "y");
        Assertions.assertEquals("{z=1, x=y}", append.toString());
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
    void listSortAsc() {
        List<String> list = new ArrayList<>();
        list.add("apple");
        list.add("google");
        list.add("android");
        List<String> sort = UtilListSort.sort(list, UtilListSort.Type.ASC);
        Assertions.assertEquals("[android, apple, google]", sort.toString());
    }

    @Test
    void listSortDesc() {
        List<String> list = new ArrayList<>();
        list.add("apple");
        list.add("google");
        list.add("android");
        List<String> sort = UtilListSort.sort(list, UtilListSort.Type.DESC);
        Assertions.assertEquals("[google, apple, android]", sort.toString());
    }

    @Test
    void listSortFieldAsc() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(new HashMapBuilder<String, Object>().append("title", "apple").append("index", 2L));
        list.add(new HashMapBuilder<String, Object>().append("title", "google").append("index", 1L));
        list.add(new HashMapBuilder<String, Object>().append("title", "android").append("index", 3L));

        List<Map<String, Object>> sort = UtilListSort.sort(list, UtilListSort.Type.ASC, map -> (Long) map.get("index"));
        Assertions.assertEquals("[{title=google, index=1}, {title=apple, index=2}, {title=android, index=3}]", sort.toString());
    }

    @Test
    void listSortFieldDesc() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(new HashMapBuilder<String, Object>().append("title", "apple").append("index", 2L));
        list.add(new HashMapBuilder<String, Object>().append("title", "google").append("index", 1L));
        list.add(new HashMapBuilder<String, Object>().append("title", "android").append("index", 3L));

        List<Map<String, Object>> sort = UtilListSort.sort(list, UtilListSort.Type.DESC, map -> (Long) map.get("index"));
        Assertions.assertEquals("[{title=android, index=3}, {title=apple, index=2}, {title=google, index=1}]", sort.toString());
    }

    @Test
    void shuffle() {
        List<String> list = new ArrayList<>();
        list.add("apple");
        list.add("google");
        list.add("android");
        UtilListSort.shuffle(list);
        System.out.println(list);
    }

    @Test
    void readUntil() {
        Assertions.assertEquals("00", Util.readUntil("00p", Util::isNumeric));
    }

    @Test
    void hashMd5() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Assertions.assertEquals("3e25960a79dbc69b674cd4ec67a72c62", Util.getHash("Hello world", "md5"));
        Assertions.assertEquals("2bf4d19c4f4cda4262b00c773779fced", Util.getHash("Привет страна", "md5"));
    }

}