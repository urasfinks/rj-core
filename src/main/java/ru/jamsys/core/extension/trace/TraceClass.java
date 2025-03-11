package ru.jamsys.core.extension.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ru.jamsys.core.App;

@JsonPropertyOrder({"start", "index", "class", "value"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TraceClass<K, V> extends Trace<K, V> {

    final Class<?> cls;

    @SuppressWarnings("unused")
    @JsonProperty("class")
    String getCls() {
        if (cls != null) {
            return App.getUniqueClassName(cls);
        }
        return null;
    }

    public TraceClass(K index, V value, Class<?> cls) {
        super(index, value);
        this.cls = cls;
    }

}
