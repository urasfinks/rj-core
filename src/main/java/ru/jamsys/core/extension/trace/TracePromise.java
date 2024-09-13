package ru.jamsys.core.extension.trace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.promise.PromiseTaskExecuteType;

import java.util.ArrayList;
import java.util.Collection;

@JsonPropertyOrder({"start", "index", "retry", "type", "value", "class", "stop"})
//@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TracePromise<K, V> extends Trace<K, V> {

    @Getter
    final Collection<TracePromise<String, ?>> taskTrace = new ArrayList<>();

    final Class<?> cls;

    @JsonProperty
    @Setter
    public Integer retry = null; //Попытка запуска задачи

    @JsonIgnore
    @Setter
    public Long timeStop;

    public String getStop() {
        return UtilDate.msFormat(timeStop);
    }

    @SuppressWarnings("unused")
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
