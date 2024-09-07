package ru.jamsys.core.extension.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Setter;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.promise.PromiseTaskExecuteType;

@JsonPropertyOrder({"index", "retry", "type", "timeAdd", "timeStop", "value", "class"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TracePromise<K, V> extends Trace<K, V> {

    final Class<?> cls;

    @JsonProperty
    @Setter
    public Integer retry = null; //Попытка запуска задачи

    @Setter
    public Long timeStop;

    public String getTimeStop() {
        return UtilDate.msFormat(timeStop);
    }

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
