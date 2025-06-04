package ru.jamsys.core.extension.log;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.victoria.metrics.VictoriaMetricsConvert;

import java.util.List;

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

    public StatDataHeader addHeader(String key, Object value) {
        header.put(key, value);
        return this;
    }

    @JsonValue
    public List<String> getValue() {
        return VictoriaMetricsConvert.getInfluxFormat(this);
    }

}
