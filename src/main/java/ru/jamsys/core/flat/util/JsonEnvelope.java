package ru.jamsys.core.flat.util;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class JsonEnvelope<O> {

    public void setObject(O object) {
        if (object == null) {
            exception = new RuntimeException("Object is empty");
        } else {
            this.object = object;
        }
    }

    O object = null;
    Exception exception = null;

}
