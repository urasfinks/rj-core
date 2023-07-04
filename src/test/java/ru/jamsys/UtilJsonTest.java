package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class UtilJsonTest {

    @Test
    void toMap() {
        WrapJsonToObject<Map<Object, Object>> mapWrapJsonToObject = UtilJson.toMap("""
                {
                    "x1":"y1",
                    "x2":"y2",
                    "x4":"y4",
                    "x3":"y3",
                    "x0":"y0"
                }
                """);
        Assertions.assertEquals("{x1=y1, x2=y2, x4=y4, x3=y3, x0=y0}", mapWrapJsonToObject.getObject().toString());
        Assertions.assertEquals("{\"x1\":\"y1\",\"x2\":\"y2\",\"x4\":\"y4\",\"x3\":\"y3\",\"x0\":\"y0\"}", UtilJson.toString(mapWrapJsonToObject.getObject(), "{}"));
    }
}