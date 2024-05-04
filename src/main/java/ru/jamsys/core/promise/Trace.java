package ru.jamsys.core.promise;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import ru.jamsys.core.util.Util;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@JsonPropertyOrder({"index", "timeAdd", "value"})
public class Trace<K, V> {

    final long timeAdd = System.currentTimeMillis();

    @Getter
    final private K index;

    final private V value;

    public String getTimeAdd() {
        return Util.msToDataFormat(timeAdd);
    }

    public Object getValue() {
        if (value instanceof Throwable) {
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
