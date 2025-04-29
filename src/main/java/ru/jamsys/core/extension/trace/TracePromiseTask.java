package ru.jamsys.core.extension.trace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.promise.PromiseTaskExecuteType;
import ru.jamsys.core.statistic.timer.nano.TimerNano;

import java.util.ArrayList;
import java.util.Collection;

@JsonPropertyOrder({"prepare", "start", "index", "class", "retry", "type", "value", "taskTrace", "stop", "nano"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TracePromiseTask<K, V> extends TraceClass<K, V> {

    @Setter
    private Long prepare;

    @Getter
    final Collection<Trace<String, ?>> taskTrace = new ArrayList<>();

    @JsonProperty
    @Setter
    public Integer retry = null; //Попытка запуска задачи

    @JsonIgnore
    @Setter
    public Long timeStop;

    @JsonIgnore
    @Setter
    private TimerNano nano;

    public String getStop() {
        return UtilDate.msFormat(timeStop);
    }

    @JsonProperty("nano")
    public Long getNano() {
        return nano != null ? nano.getOffsetLastActivityNano() : null;
    }

    @SuppressWarnings("unused")
    public String getPrepare() {
        return UtilDate.msFormat(prepare);
    }

    @JsonProperty
    @SuppressWarnings("FieldCanBeLocal")
    final private PromiseTaskExecuteType type;

    public TracePromiseTask(K index, V value, PromiseTaskExecuteType type, Class<?> cls) {
        super(index, value, cls);
        this.type = type;
    }

}
