package ru.jamsys.util;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class JsonEnvelope<T> {

    public void setObject(T object) {
        if (object == null) {
            exception = new RuntimeException("Object is empty");
        } else {
            this.object = object;
        }
    }

    T object = null;
    Exception exception = null;

}
