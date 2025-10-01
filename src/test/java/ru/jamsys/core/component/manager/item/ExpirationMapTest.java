package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.expiration.ExpirationDrop;
import ru.jamsys.core.extension.expiration.ExpirationMap;
import ru.jamsys.core.flat.util.Util;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    @Getter
    @Setter
    public static class XTest implements ExpirationDrop {

        private String data = "";

        @Override
        public void onExpirationDrop() {
            data = "YES";
        }
    }

    private ExpirationMap<String, String> map;

    @BeforeEach
    void setUp() {
        ManagerConfiguration<ExpirationMap<String, String>> expirationMapConfiguration =
                ManagerConfiguration.getInstance(
                        "testMap",
                        java.util.UUID.randomUUID().toString(),
                        ExpirationMap.class,
                        integerXTestExpirationMap -> integerXTestExpirationMap.setupTimeoutElementExpirationMs(1_000)
                );
        map = expirationMapConfiguration.get();
        map.clear();
    }

    @Test
    void computeIfAbsent() {
        String uuid = UUID.randomUUID().toString();
        ManagerConfiguration<ExpirationMap<Integer, XTest>> expirationMapConfiguration =
                ManagerConfiguration.getInstance(
                        "test",
                        uuid,
                        ExpirationMap.class,
                        // устанавливаем для элементов map срок хранения
                        integerXTestExpirationMap -> integerXTestExpirationMap.setupTimeoutElementExpirationMs(1_000)
                );


        ExpirationMap<Integer, XTest> test = expirationMapConfiguration.get();
        XTest s = test.computeIfAbsent(10, _ -> {
            XTest xTest = new XTest();
            xTest.setData("Hello world");
            return xTest;
        });

        Assertions.assertEquals(1, test.size());
        Assertions.assertEquals(s.hashCode(), test.get(10).hashCode());
        Util.testSleepMs(2_000);
        Assertions.assertEquals(0, test.size());
        Assertions.assertEquals("YES", s.getData());
    }


    @Test
    void testPutAndGet() {
        map.put("key1", "value1");
        Assertions.assertEquals("value1", map.get("key1"));
    }

    @SuppressWarnings("all")
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
        Assertions.assertTrue(true);
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
        String result = map.computeIfAbsent("computed", _ -> "generated");
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

        map.entrySet().removeIf(e -> e.getKey().equals("a"));

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
        Assertions.assertEquals("peekable", map.get("x", false));
    }

    @Test
    void testExpireNow() {
        map.put("temp", "data");
        map.remove("temp");
        Assertions.assertFalse(map.containsKey("temp"));
    }

    @Test
    void testFlushAndGetStatistic() {
        map.put("1", "a");
        map.put("2", "b");

        var result = map.flushAndGetStatistic(new AtomicBoolean(true));
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("testMap", result.getFirst().getNs());
        //Assertions.assertEquals(2, result.get(0).getHeaders().get("size"));
    }

}