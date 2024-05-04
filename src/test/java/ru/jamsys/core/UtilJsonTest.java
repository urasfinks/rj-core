package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.util.UtilJson;
import ru.jamsys.core.util.JsonEnvelope;

import java.util.Map;

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
}