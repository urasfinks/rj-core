package ru.jamsys.core.extension.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.line.writer.LineWriterList;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.statistic.timer.nano.TimerNanoEnvelope;

@JsonPropertyOrder({"index", "timeAdd", "value"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Trace<K, V> {

    final public long timeAdd = System.currentTimeMillis();

    @Getter
    final protected K index;

    @Setter
    protected V value;

    public String getTimeAdd() {
        return Util.msToDataFormat(timeAdd);
    }

    public Object getValue() {
        if (value != null) {
            if (value instanceof Throwable) {
                LineWriterList lineWriterList = new LineWriterList();
                App.get(ExceptionHandler.class).getTextException((Throwable) value, lineWriterList);
                return lineWriterList.getResult();
            } else if (value instanceof TimerNanoEnvelope) {
                return new HashMapBuilder<String, Object>()
                        .append("nano", ((TimerNanoEnvelope) value).getOffsetLastActivityNano());
            }
        }
        return value;
    }

    public Trace(K index, V value) {
        this.index = index;
        this.value = value;
    }

}
