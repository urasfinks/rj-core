package ru.jamsys.core;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.flat.util.*;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// IO time: 13ms
// COMPUTE time: 10ms

class UtilTest {

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
    void listReversed() {
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
        UtilLog.printInfo(list);
    }

    @Test
    void hashMd5() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Assertions.assertEquals("3e25960a79dbc69b674cd4ec67a72c62", Util.getHash("Hello world", "md5"));
        Assertions.assertEquals("2bf4d19c4f4cda4262b00c773779fced", Util.getHash("Привет страна", "md5"));
    }

    @Getter
    @Setter
    public static class Tmp {
        String y;
    }

    @Test
    void mapToObject() {
        Tmp tmp = UtilJson.mapToObject(new HashMapBuilder<String, Object>()
                        .append("y", "x"),
                Tmp.class
        );
        Assertions.assertEquals("x", tmp.getY());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Getter
    @FieldNameConstants
    public static class FileByteRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

        @SuppressWarnings("all")
        @PropertyKey("folder")
        private String folder = "LogManager";

        @SuppressWarnings("all")
        @PropertyKey("file.size.kb")
        private Integer fileSizeKb = 20971520;

        @SuppressWarnings("all")
        @PropertyKey("file.count")
        private Integer fileCount = 100;

        @PropertyNotNull
        @PropertyKey("file.name")
        private String fileName;

    }

    @Test
    void test(){
        FileByteRepositoryProperty fileByteRepositoryProperty = new FileByteRepositoryProperty();
        UtilLog.printInfo(fileByteRepositoryProperty.getByFieldNameConstants(FileByteRepositoryProperty.Fields.folder));
    }

    @Test
    void testResetLastNDigits2() {
        long l = System.currentTimeMillis();       // 1746263615151
        long sec = Util.resetLastNDigits(l, 3);  // 1746263615000
        Assertions.assertTrue(l - sec < 1000);
    }

    @Test
    public void testGenUserFormat() {
        String user = Util.genUser();
        Assertions.assertTrue(user.matches("u\\d{5}"));
    }

    @Test
    public void testGenPasswordFormat() {
        String pass = Util.genPassword();
        Assertions.assertTrue(pass.startsWith("p"));
        Assertions.assertTrue(pass.length() >= 9);
    }

    @Test
    public void testHashConsistency() throws Exception {
        String data = "test123";
        String hash1 = Util.getHash(data, "MD5");
        String hash2 = Util.getHash(data, "MD5");
        Assertions.assertEquals(hash1, hash2);
    }

    @Test
    public void testIsNumeric() {
        Assertions.assertTrue(Util.isNumeric("123.45"));
        Assertions.assertFalse(Util.isNumeric("abc"));
    }

    @Test
    public void testIsInt() {
        Assertions.assertTrue(Util.isInt("42"));
        Assertions.assertFalse(Util.isInt("42.1"));
        Assertions.assertFalse(Util.isInt("abc"));
    }

    @Test
    public void testRandomWithinRange() {
        for (int i = 0; i < 100; i++) {
            int val = Util.random(10, 20);
            Assertions.assertTrue(val >= 10 && val <= 20);
        }
    }

    @Test
    public void testResetLastNDigits() {
        Assertions.assertEquals(12000, Util.resetLastNDigits(12345, 3));
        Assertions.assertEquals(0, Util.resetLastNDigits(999, 3));
    }

    @Test
    public void testStringToIntConsistency() {
        int val1 = Util.stringToInt("test", 0, 100);
        int val2 = Util.stringToInt("test", 0, 100);
        Assertions.assertEquals(val1, val2);
    }

    @Test
    public void testOverflow() {
        Map<String, Object> base = new HashMap<>();
        base.put("a", 1);
        Map<String, Object> update = new HashMap<>();
        update.put("a", 2);
        update.put("b", 3);
        Util.overflow(base, update);
        Assertions.assertEquals(2, base.get("a"));
        Assertions.assertEquals(3, base.get("b"));
    }

    @Test
    public void testGetConcurrentHashSet() {
        Set<String> set = Util.getConcurrentHashSet();
        Assertions.assertNotNull(set);
        Assertions.assertTrue(set.add("value"));
    }

    @Test
    public void testAwaitFlagTimeoutFalse() {
        AtomicBoolean flag = new AtomicBoolean(true);
        boolean result = Util.await(flag, 50, "Timeout occurred");
        Assertions.assertFalse(result);
    }

    @Test
    public void testAwaitFlagSetToFalse() {
        AtomicBoolean flag = new AtomicBoolean(true);
        new Thread(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
            flag.set(false);
        }).start();
        boolean result = Util.await(flag, 100, "Should succeed");
        Assertions.assertTrue(result);
    }

    @Test
    void testNullInputReturnsMin() {
        int result = Util.stringToInt(null, 10, 20);
        Assertions.assertEquals(10, result);
    }

    @Test
    void testEmptyStringIsInRange() {
        int result = Util.stringToInt("", 0, 5);
        Assertions.assertTrue(result >= 0 && result <= 5);
    }

    @Test
    void testConsistentHashing() {
        int a = Util.stringToInt("hello", 100, 200);
        int b = Util.stringToInt("hello", 100, 200);
        Assertions.assertEquals(a, b);
    }

    @Test
    void testDifferentInputsProduceDifferentResults() {
        int a = Util.stringToInt("foo", 0, 1000);
        int b = Util.stringToInt("bar", 0, 1000);
        Assertions.assertNotEquals(a, b); // допускается коллизия, но маловероятна
    }

    @Test
    void testOutputAlwaysInRange() {
        for (int i = 0; i < 100; i++) {
            String input = "test" + i;
            int result = Util.stringToInt(input, 10, 50);
            Assertions.assertTrue(result >= 10 && result <= 50, "Result out of range: " + result);
        }
    }

    @Test
    void testMinEqualsMaxAlwaysReturnsMin() {
        int result = Util.stringToInt("any", 7, 7);
        Assertions.assertEquals(7, result);
    }

    @Test
    void testMinGreaterThanMaxReturnsMin() {
        int result = Util.stringToInt("value", 20, 10);
        Assertions.assertEquals(20, result);
    }

    @Test
    void testSpreadDistribution() {
        int min = 0, max = 10;
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            int value = Util.stringToInt("key" + i, min, max);
            seen.add(value);
        }
        // хотя бы 8 разных значений из 11 возможных — приемлемо
        Assertions.assertTrue(seen.size() >= 8, "Distribution not varied enough: " + seen);
    }

    @Test
    public void testCartesianTwoLists() {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<String> list2 = Arrays.asList("a", "b");

        Collection<List<?>> result = Util.cartesian(ArrayList::new, list1, list2);

        Set<String> expected = Set.of(
                "[1, a]",
                "[1, b]",
                "[2, a]",
                "[2, b]"
        );

        Set<String> actual = new HashSet<>();
        for (List<?> combo : result) {
            actual.add(combo.toString());
        }

        Assertions.assertEquals(expected, actual);
    }

    @SuppressWarnings("all")
    @Test
    public void testCartesianSingleList() {
        List<Integer> list = Arrays.asList(10, 20, 30);
        Collection<List<?>> result = Util.cartesian(ArrayList::new, list);

        Assertions.assertEquals(3, result.size());
        for (List<?> combo : result) {
            Assertions.assertEquals(1, combo.size());
            Assertions.assertTrue(list.contains(combo.getFirst()));
        }
    }

    @Test
    public void testCartesianEmptyInput() {
        Collection<List<?>> result = Util.cartesian(ArrayList::new);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @SuppressWarnings("all")
    @Test
    public void testCartesianNullSupplier() {
        List<Integer> list1 = Arrays.asList(1, 2);
        Collection<List<?>> result = Util.cartesian(null, list1);
        Assertions.assertNull(result);
    }

}