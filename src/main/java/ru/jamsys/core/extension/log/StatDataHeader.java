package ru.jamsys.core.extension.log;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public class StatDataHeader extends DataHeader {

    private final String cls;
    private final String ns;

    public StatDataHeader(Class<?> cls, String ns) {
        this.cls = App.getUniqueClassName(cls);
        this.ns = ns;
    }

    @Getter
    @Setter
    public static class Measurement {

        private Map<String, Object> map = new LinkedHashMap<>();

        private Map<String, Object> metric = new LinkedHashMap<>();

        public Measurement(String name, Map<String, String> labels, Object value) {
            metric.put("__name__", name);
            metric.putAll(labels);
            map.put("metric", metric);
            map.put("values", List.of(value));
            map.put("timestamps", System.currentTimeMillis());
        }

        @JsonValue
        public Object getValue() {
            return map;
        }

        public static List<Measurement> from(StatDataHeader statDataHeader) {
            List<Measurement> result = new ArrayList<>();
            Map<String, Object> header1 = statDataHeader.getHeader();
            for (String key : header1.keySet()) {
                result.add(new Measurement(
                        CascadeKey.complex(statDataHeader.getCls(), key),
                        new HashMapBuilder<String, String>()
                                .append("ns", statDataHeader.getNs()),
                        header1.get(key)
                ));
            }
            return result;
        }

    }

    @JsonValue
    public List<Measurement> getValue() {
        return Measurement.from(this);
    }

}
