package ru.jamsys.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jayway.jsonpath.ReadContext;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

// IO time: 109ms
// COMPUTE time: 112ms

class UtilJsonTest {

    @Test
    void toMap() throws Throwable {
        Map<String, Object> mapJsonEnvelope = UtilJson.getMapOrThrow("""
                {
                    "x1":"y1",
                    "x2":"y2",
                    "x4":"y4",
                    "x3":"y3",
                    "x0":"y0"
                }
                """);
        Assertions.assertEquals("{x1=y1, x2=y2, x4=y4, x3=y3, x0=y0}", mapJsonEnvelope.toString());
        Assertions.assertEquals("{\"x1\":\"y1\",\"x2\":\"y2\",\"x4\":\"y4\",\"x3\":\"y3\",\"x0\":\"y0\"}", UtilJson.toString(mapJsonEnvelope, "{}"));
    }

    @Test
    void selector() {
        String json = """
                {
                    "x1":"y1",
                    "x2":"y2",
                    "x4":"y4",
                    "x3":"y3",
                    "x0":"y0",
                    "n": 1,
                    "list" : ["Hello", "World"],
                    "list2" : [{"x":"y"}, {"x2":"y2"}]
                }
                """;
        ReadContext context = UtilJson.getContext(json);
        Assertions.assertEquals("String", context.read("$.x1").getClass().getSimpleName());
        Assertions.assertEquals("Integer", context.read("$.n").getClass().getSimpleName());
        Assertions.assertEquals("JSONArray", context.read("$.list").getClass().getSimpleName());
        Assertions.assertEquals("String", context.read("$.list[0]").getClass().getSimpleName());
        Assertions.assertEquals("String", context.read("$.list2[0].x").getClass().getSimpleName());
        Assertions.assertEquals("y", context.read("$.list2[0].x"));


        Map<String, Object> result = new LinkedHashMap<>();
        UtilJson.selector(json, new HashMapBuilder<String, String>()
                        .append("fio", "$.x1")
                        .append("s", "$.x2")
                , result);

        Assertions.assertEquals("{fio=y1, s=y2}", result.toString());

    }

    @Test
    void logObjectAtTheMoment() {
        Assertions.assertEquals("{hello=world}", UtilJson.toLog(new X()).toString());
        Assertions.assertEquals("hello", UtilJson.toLog("hello").toString());
        Assertions.assertEquals("", UtilJson.toLog("").toString());
        Assertions.assertNull(UtilJson.toLog(null));
        ArrayList<Object> list = new ArrayList<>();
        Assertions.assertEquals("[]", UtilJson.toLog(list).toString());
        list.add("hello");
        Assertions.assertEquals("[hello]", UtilJson.toLog(list).toString());
        list.add("world");
        Assertions.assertEquals("[hello, world]", UtilJson.toLog(list).toString());
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class X {
        private final String hello = "world";
    }
}