package ru.jamsys.core.extension.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.util.Util;

import java.util.ArrayList;
import java.util.List;

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
        if (value != null && value instanceof Throwable) {
            StackTraceElement[] elements = ((Throwable) value).getStackTrace();
            List<String> result = new ArrayList<>();
            result.add(value.getClass().getName() + " " + ((Throwable) value).getMessage());
            for (StackTraceElement element : elements) {
                result.add(
                        "at "
                                + element.getClassName() + "." + element.getMethodName()
                                + "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
            }
            return result;
        }
        return value;
    }

    public Trace(K index, V value) {
        this.index = index;
        this.value = value;
    }

}
