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
import ru.jamsys.core.flat.UtilCodeStyle;
import ru.jamsys.core.flat.util.*;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// IO time: 13ms
// COMPUTE time: 10ms

class UtilTest {

    @Test
    void snakeToCamel() {
        Assertions.assertEquals("HelloWorld", UtilCodeStyle.snakeToCamel("HELLO_WORLD"));
        Assertions.assertEquals("HelloWorld1", UtilCodeStyle.snakeToCamel("HELLO_WORLD1"));
        Assertions.assertEquals("HelloWorld1", UtilCodeStyle.snakeToCamel("HELLO_WORLD_1"));
        Assertions.assertEquals("HelloWorld1_", UtilCodeStyle.snakeToCamel("HELLO_WORLD_1_"));
        Assertions.assertEquals("_HelloWorld1_", UtilCodeStyle.snakeToCamel("_HELLO_WORLD_1_"));
        Assertions.assertEquals("Helloworld", UtilCodeStyle.snakeToCamel("HELLOWORLD"));
        Assertions.assertEquals("DataType", UtilCodeStyle.snakeToCamel("dataType"));
        Assertions.assertEquals("DataType", UtilCodeStyle.snakeToCamel("DataType"));
        Assertions.assertEquals("DataType", UtilCodeStyle.snakeToCamel("data_Type"));
        Assertions.assertEquals("DataType", UtilCodeStyle.snakeToCamel("data_type"));
    }

    @Test
    void camelToSnake() {
        Assertions.assertEquals("HELLO_WORLD", UtilCodeStyle.camelToSnake("HelloWorld"));
        Assertions.assertEquals("_HELLOWORLD", UtilCodeStyle.camelToSnake("_HelloWorld"));
        Assertions.assertEquals("HELLO_WORLD1", UtilCodeStyle.camelToSnake("HelloWorld1"));
        Assertions.assertEquals("HELLOWORLD", UtilCodeStyle.camelToSnake("Helloworld"));
        Assertions.assertEquals("HELLO_WORLD", UtilCodeStyle.camelToSnake("Hello_world"));
    }

    @Test
    void firstCharToUpperCase() {
        Assertions.assertEquals("HelloWorld", UtilText.firstCharToUpperCase("HelloWorld"));
        Assertions.assertEquals("HelloWorld", UtilText.firstCharToUpperCase("helloWorld"));
        Assertions.assertEquals("", UtilText.firstCharToUpperCase(""));
        Assertions.assertEquals("U", UtilText.firstCharToUpperCase("u"));
        Assertions.assertEquals("Ur", UtilText.firstCharToUpperCase("ur"));
        Assertions.assertEquals("U", UtilText.firstCharToUpperCase("U"));
        Assertions.assertEquals("UR", UtilText.firstCharToUpperCase("UR"));
        Assertions.assertEquals("URa", UtilText.firstCharToUpperCase("URa"));
        Assertions.assertNull(UtilText.firstCharToUpperCase(null));
    }

    @Test
    void firstCharToLowerCase() {
        Assertions.assertEquals("helloWorld", UtilText.firstCharToLowerCase("HelloWorld"));
        Assertions.assertEquals("helloWorld", UtilText.firstCharToLowerCase("helloWorld"));
        Assertions.assertEquals("", UtilText.firstCharToLowerCase(""));
        Assertions.assertEquals("u", UtilText.firstCharToLowerCase("U"));
        Assertions.assertEquals("ur", UtilText.firstCharToLowerCase("Ur"));
        Assertions.assertEquals("u", UtilText.firstCharToLowerCase("u"));
        Assertions.assertEquals("ur", UtilText.firstCharToLowerCase("ur"));
        Assertions.assertEquals("urA", UtilText.firstCharToLowerCase("urA"));
        Assertions.assertNull(UtilText.firstCharToLowerCase(null));
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
    void readUntil() {
        Assertions.assertEquals("00", UtilText.readUntil("00p", Util::isNumeric));
    }

    @Test
    void hashMd5() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Assertions.assertEquals("3e25960a79dbc69b674cd4ec67a72c62", Util.getHash("Hello world", "md5"));
        Assertions.assertEquals("2bf4d19c4f4cda4262b00c773779fced", Util.getHash("Привет страна", "md5"));
    }

    @Test
    void digitTranslate() {
        Assertions.assertEquals("коров", UtilText.digitTranslate(5, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(25, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(35, "корова", "коровы", "коров"));

        Assertions.assertEquals("корова", UtilText.digitTranslate(1, "корова", "коровы", "коров"));
        Assertions.assertEquals("коровы", UtilText.digitTranslate(2, "корова", "коровы", "коров"));
        Assertions.assertEquals("коровы", UtilText.digitTranslate(3, "корова", "коровы", "коров"));
        Assertions.assertEquals("коровы", UtilText.digitTranslate(4, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(6, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(7, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(8, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(9, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(10, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(11, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(12, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(13, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(14, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(15, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(16, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(17, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(18, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(19, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(20, "корова", "коровы", "коров"));
        Assertions.assertEquals("корова", UtilText.digitTranslate(21, "корова", "коровы", "коров"));
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
    void testResetLastNDigits(){
        long l = System.currentTimeMillis();       // 1746263615151
        long sec = Util.resetLastNDigits(l, 3);  // 1746263615000
        Assertions.assertTrue(l - sec < 1000);
    }

}