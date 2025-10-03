package ru.jamsys.core.extension.log;

import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.victoria.metrics.VictoriaMetricsLineProtocolBuilder;
import ru.jamsys.core.flat.util.UtilLog;

import static org.junit.jupiter.api.Assertions.*;

class VictoriaMetricsLineProtocolBuilderTest {

    @Test
    public void testBuildWithSingleLabelAndTimestamp() {
        String line = new VictoriaMetricsLineProtocolBuilder()
                .setMeasurement("myMetric")
                .addLabel("region", "us-west")
                .setValue(123.45)
                .setTimestampMillis(1000)
                .build();

        assertEquals("myMetric,region=us-west value=123.45 1000", line);
    }

    @Test
    public void testBuildWithMultipleLabels() {
        String line = new VictoriaMetricsLineProtocolBuilder()
                .setMeasurement("cpu_load")
                .addLabel("host", "server1")
                .addLabel("env", "prod")
                .setValue(0.99)
                .setTimestampMillis(2000)
                .build();

        // Labels order сохранится, т.к. LinkedHashMap
        assertEquals("cpu_load,host=server1,env=prod value=0.99 2000", line);
    }

    @Test
    public void testBuildWithoutTimestamp() {
        String line = new VictoriaMetricsLineProtocolBuilder()
                .setMeasurement("requests")
                .addLabel("status", "200")
                .setValue(500)
                .build();

        // Нет timestamp, значит отсутствует в строке
        assertEquals("requests,status=200 value=500.0", line);
    }

    @Test
    public void testEscapeMeasurement() {
        String line = new VictoriaMetricsLineProtocolBuilder()
                .setMeasurement("mea,s ure=ment")
                .addLabel("key", "val")
                .setValue(1)
                .build();
        UtilLog.printInfo(line);
        // Запятая, пробел и равно в measurement должны быть экранированы
        assertTrue(line.startsWith("mea\\,s\\ ure\\=ment,"));
    }

    @Test
    public void testEscapeLabelKeyAndValue() {
        String line = new VictoriaMetricsLineProtocolBuilder()
                .setMeasurement("metric")
                .addLabel("la,bel=key", "va lue=1")
                .setValue(2)
                .build();

        // Проверяем экранирование в label key и value
        assertTrue(line.contains("la\\,bel\\=key=va\\ lue\\=1"));
    }

    @Test
    public void testZeroTimestamp() {
        String line = new VictoriaMetricsLineProtocolBuilder()
                .setMeasurement("metric")
                .addLabel("k", "v")
                .setValue(1)
                .setTimestampMillis(0)
                .build();

        // При нулевом timestamp поле не выводится
        assertEquals("metric,k=v value=1.0", line);
    }

    @Test
    public void testMissingMeasurementThrows() {
        VictoriaMetricsLineProtocolBuilder builder = new VictoriaMetricsLineProtocolBuilder()
                .addLabel("k", "v")
                .setValue(1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Measurement must be set", ex.getMessage());
    }

    @Test
    public void testValueCanBeNegativeOrZero() {
        String lineNegative = new VictoriaMetricsLineProtocolBuilder()
                .setMeasurement("metric")
                .setValue(-123.45)
                .build();

        assertTrue(lineNegative.contains("value=-123.45"));

        String lineZero = new VictoriaMetricsLineProtocolBuilder()
                .setMeasurement("metric")
                .setValue(0)
                .build();

        assertTrue(lineZero.contains("value=0.0"));
    }

    @Test
    public void testLabelsPreserveOrder() {
        VictoriaMetricsLineProtocolBuilder builder = new VictoriaMetricsLineProtocolBuilder()
                .setMeasurement("metric")
                .addLabel("b", "2")
                .addLabel("a", "1")
                .setValue(10);

        String line = builder.build();
        // Порядок лейблов должен быть "b=2,a=1"
        assertTrue(line.startsWith("metric,b=2,a=1"));
    }
}