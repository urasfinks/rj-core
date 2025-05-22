package ru.jamsys.core.component.manager.item;

import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.expiration.ExpirationMap;
import ru.jamsys.core.flat.util.Util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

class ExpirationMapTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    public static class XTest {

    }

    @Test
    void computeIfAbsent() {
        Manager.Configuration<ExpirationMap<Integer, XTest>> expirationMapConfiguration =
                ExpirationMap.getInstanceConfigure("test", 1000);
        ExpirationMap<Integer, XTest> test = expirationMapConfiguration.get();
        XTest s = test.computeIfAbsent(10, _ -> new XTest());

        Assertions.assertEquals(1, test.size());
        //Assertions.assertEquals(1, test.getExpirationMap().size());
        Assertions.assertEquals(s.hashCode(), test.get(10).hashCode());
        Assertions.assertEquals(s.hashCode(), test.get(10).hashCode());
        Util.testSleepMs(2000);
        Assertions.assertEquals(0, test.size());
        //Assertions.assertEquals(0, test.getExpirationMap().size());
    }

    private ExpirationMap<String, String> map;

    @BeforeEach
    void setUp() {
        map = ExpirationMap.getInstanceConfigure("testMap", 10_000).getGeneric();
        map.clear();
    }

    @Test
    void testPutAndGet() {
        map.put("key1", "value1");
        Assertions.assertEquals("value1", map.get("key1"));
    }

    @Test
    void testOverwrite() {
        map.put("key1", "value1");
        map.put("key1", "value2");
        Assertions.assertEquals("value2", map.get("key1"));
        Assertions.assertEquals(1, map.size());
    }

    @Test
    void testRemove() {
        map.put("key1", "value1");
        String removed = map.remove("key1");
        Assertions.assertEquals("value1", removed);
        Assertions.assertNull(map.get("key1"));
    }

    @Test
    void testClear() {
        map.put("a", "1");
        map.put("b", "2");
        map.clear();
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    void testContainsKeyAndValue() {
        map.put("a", "valueA");
        Assertions.assertTrue(map.containsKey("a"));
        Assertions.assertTrue(map.containsValue("valueA"));
        Assertions.assertFalse(map.containsValue("valueX"));
    }

    @Test
    void testComputeIfAbsent() {
        String result = map.computeIfAbsent("computed", k -> "generated");
        Assertions.assertEquals("generated", result);
        Assertions.assertEquals("generated", map.get("computed"));
    }

    @Test
    void testEntrySetBehavior() {
        map.put("one", "1");
        map.put("two", "2");

        Set<Map.Entry<String, String>> entries = map.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (entry.getKey().equals("one")) {
                entry.setValue("uno");
            }
        }

        Assertions.assertEquals("uno", map.get("one"));
    }

    @Test
    void testEntrySetRemove() {
        map.put("a", "1");
        map.put("b", "2");

        var it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getKey().equals("a")) {
                it.remove();
            }
        }

        Assertions.assertFalse(map.containsKey("a"));
        Assertions.assertTrue(map.containsKey("b"));
    }

    @Test
    void testKeySetRemove() {
        map.put("x", "valX");
        map.put("y", "valY");

        map.keySet().remove("x");
        Assertions.assertFalse(map.containsKey("x"));
        Assertions.assertTrue(map.containsKey("y"));
    }

    @Test
    void testValuesRemove() {
        map.put("a", "val1");
        map.put("b", "val2");

        map.values().remove("val1");
        Assertions.assertFalse(map.containsValue("val1"));
        Assertions.assertTrue(map.containsValue("val2"));
    }

    @Test
    void testPeek() {
        map.put("x", "peekable");
        Assertions.assertEquals("peekable", map.peek("x"));
    }

    @Test
    void testExpireNow() {
        map.put("temp", "data");
        Assertions.assertTrue(map.expireNow("temp"));
        Assertions.assertFalse(map.containsKey("temp"));
    }

    @Test
    void testFlushAndGetStatistic() {
        map.put("1", "a");
        map.put("2", "b");

        var result = map.flushAndGetStatistic(new AtomicBoolean(true));
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("testMap", result.getFirst().getBody());
        //Assertions.assertEquals(2, result.get(0).getHeaders().get("size"));
    }

}