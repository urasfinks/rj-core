package ru.jamsys.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jayway.jsonpath.ReadContext;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.JsonEnvelope;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.statistic.Statistic;

import java.util.LinkedHashMap;
import java.util.Map;

// IO time: 109ms
// COMPUTE time: 112ms

class UtilJsonTest {

    @Test
    void toMap() {
        JsonEnvelope<Map<Object, Object>> mapJsonEnvelope = UtilJson.toMap("""
                {
                    "x1":"y1",
                    "x2":"y2",
                    "x4":"y4",
                    "x3":"y3",
                    "x0":"y0"
                }
                """);
        Assertions.assertEquals("{x1=y1, x2=y2, x4=y4, x3=y3, x0=y0}", mapJsonEnvelope.getObject().toString());
        Assertions.assertEquals("{\"x1\":\"y1\",\"x2\":\"y2\",\"x4\":\"y4\",\"x3\":\"y3\",\"x0\":\"y0\"}", UtilJson.toString(mapJsonEnvelope.getObject(), "{}"));
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
    void wrapUnwrap() {
        Statistic statistic1 = new Statistic();
        statistic1.addTag("tag", "1");
        statistic1.addField("field", 2);

        String x = UtilJson.toString(statistic1, "{}");

        JsonEnvelope<Statistic> objectOverflow = UtilJson.toObjectOverflow(x, Statistic.class);

        Assertions.assertNull(objectOverflow.getException());

        Statistic statistic2 = objectOverflow.getObject();

        Assertions.assertEquals(1, statistic2.getFields().size());
        Assertions.assertEquals(1, statistic2.getTags().size());

        Assertions.assertEquals(x, UtilJson.toString(statistic2, "{}"));

    }

    @Test
    void logObjectAtTheMoment() {
        Assertions.assertEquals("{hello=world}", UtilJson.toLog(new X()).toString());
        Assertions.assertEquals("hello", UtilJson.toLog("hello").toString());
        Assertions.assertEquals("", UtilJson.toLog("").toString());
        Assertions.assertNull(UtilJson.toLog(null));
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class X {
        private final String hello = "world";
    }
}