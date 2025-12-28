package ru.jamsys.core.extension.trace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.line.writer.LineWriterList;
import ru.jamsys.core.flat.util.date.UtilDate;

public class Trace<K, V> {

    @JsonIgnore
    final public long timeAdd = System.currentTimeMillis();

    @Getter
    final protected K index;

    @Setter
    private V value;

    public String getStart() {
        return UtilDate.millis(timeAdd).toDate().getDate();
    }

    @SuppressWarnings("unused")
    public V getValueRaw() {
        return value;
    }

    public Object getValueWrap() {
        if (value instanceof Throwable valueCast) {
            LineWriterList lineWriterList = new LineWriterList();
            ExceptionHandler.getTextException(valueCast, lineWriterList);
            return lineWriterList.getResult();
        }
        return value;
    }

    @JsonValue
    public Object getJsonValue() {
        if (value == null) {
            return UtilDate.millis(timeAdd).toDate().getDate() + " " + index;
        } else {
            return new HashMapBuilder<String, Object>()
                    .append(UtilDate.millis(timeAdd).toDate().getDate() + " " + index, getValueWrap());
        }
    }

    public Trace(K index, V value) {
        this.index = index;
        this.value = value;
    }

}
