package ru.jamsys.core.extension.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ru.jamsys.core.promise.PromiseTaskExecuteType;

@JsonPropertyOrder({"index", "type", "timeAdd", "value", "class"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TracePromise<K, V> extends Trace<K, V> {

    final Class<?> cls;

    @JsonProperty("class")
    String getCls() {
        if (cls != null) {
            return cls.getSimpleName();
        }
        return null;
    }

    @JsonProperty
    @SuppressWarnings("FieldCanBeLocal")
    final private PromiseTaskExecuteType type;

    public TracePromise(K index, V value, PromiseTaskExecuteType type, Class<?> cls) {
        super(index, value);
        this.type = type;
        this.cls = cls;
    }

}
