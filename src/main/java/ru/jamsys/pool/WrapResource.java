package ru.jamsys.pool;

import lombok.Data;

@Data
public class WrapResource<T> {

    private T resource;
    private long lastRun = System.currentTimeMillis();

}
