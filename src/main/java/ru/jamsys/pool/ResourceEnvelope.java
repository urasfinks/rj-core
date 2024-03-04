package ru.jamsys.pool;

import lombok.Data;

@Data
public class ResourceEnvelope<T> {

    private T resource;
    private long lastRunMs = System.currentTimeMillis();

}
