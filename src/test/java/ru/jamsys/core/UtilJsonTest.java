package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.JsonEnvelope;

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
}