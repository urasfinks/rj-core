package ru.jamsys;

import lombok.Data;

@Data
public class WrapJsonToObject<T> {

    T object = null;
    Exception exception = null;

}
