package ru.jamsys.core.extension.trace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.extension.line.writer.LineWriterList;
import ru.jamsys.core.flat.util.UtilDate;

@JsonPropertyOrder({"start", "index", "value"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Trace<K, V> {

    @JsonIgnore
    final public long timeAdd = System.currentTimeMillis();

    @Getter
    final protected K index;

    @Setter
    private V value;

    public String getStart() {
        return UtilDate.msFormat(timeAdd);
    }

    @SuppressWarnings("unused")
    @JsonIgnore
    public V getValueRaw() {
        return value;
    }

    public Object getValue() {
        if (value != null) {
            if (value instanceof Throwable valueCast) {
                LineWriterList lineWriterList = new LineWriterList();
                ExceptionHandler.getTextException(valueCast, lineWriterList);
                return lineWriterList.getResult();
            }
        }
        return value;
    }

    public Trace(K index, V value) {
        this.index = index;
        this.value = value;
    }

}
