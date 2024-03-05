package ru.jamsys.pool;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceEnvelope<T> {

    private T resource;
    private long lastRunMs = System.currentTimeMillis();

}
