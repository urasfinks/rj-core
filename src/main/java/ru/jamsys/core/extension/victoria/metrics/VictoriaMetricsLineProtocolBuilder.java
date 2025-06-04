package ru.jamsys.core.extension.victoria.metrics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public class VictoriaMetricsLineProtocolBuilder {

    private String measurement;

    private final Map<String, String> labels = new LinkedHashMap<>();

    private double value;

    private long timestampMillis;

    public VictoriaMetricsLineProtocolBuilder setMeasurement(String measurement) {
        this.measurement = escapeKey(measurement);
        return this;
    }

    public VictoriaMetricsLineProtocolBuilder addLabel(String key, String val) {
        labels.put(escapeKey(key), escapeLabelValue(val));
        return this;
    }

    public VictoriaMetricsLineProtocolBuilder setValue(double value) {
        this.value = value;
        return this;
    }

    public VictoriaMetricsLineProtocolBuilder setTimestampMillis(long timestampMillis) {
        this.timestampMillis = timestampMillis;
        return this;
    }

    public String build() {
        if (measurement == null || measurement.isEmpty()) {
            throw new IllegalStateException("Measurement must be set");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(measurement);

        if (!labels.isEmpty()) {
            StringJoiner sj = new StringJoiner(",");
            for (Map.Entry<String, String> e : labels.entrySet()) {
                sj.add(e.getKey() + "=" + e.getValue());
            }
            sb.append(",").append(sj);
        }

        sb.append(" value=").append(value);

        if (timestampMillis > 0) {
            sb.append(" ").append(timestampMillis); // ms -> ns
        }

        return sb.toString();
    }

    // Экранирование measurement и label keys: запятая, пробел, равенство
    private String escapeKey(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(" ", "\\ ")
                .replace("=", "\\=");
    }

    // Экранирование label values (дополнительно экранируем кавычки, если будут)
    private String escapeLabelValue(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(" ", "\\ ")
                .replace("=", "\\=");
    }

}
