package ru.jamsys.core.extension.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.PromiseTaskExecuteType;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@JsonPropertyOrder({"index", "type", "timeAdd", "value", "class"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Trace<K, V> {

    final long timeAdd = System.currentTimeMillis();

    @Getter
    final private K index;

    @Setter
    private V value;

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

    public Trace(K index, V value, PromiseTaskExecuteType type, Class<?> cls) {
        this.index = index;
        this.value = value;
        this.type = type;
        this.cls = cls;
    }

}
