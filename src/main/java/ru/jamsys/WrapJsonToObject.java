package ru.jamsys;

import lombok.Data;

@Data
public class WrapJsonToObject<T> {

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
