package ru.jamsys.pool;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceEnvelope<T> {

    private T resource;
    private long lastRunMs = System.currentTimeMillis();

    public ResourceEnvelope(T resource) {
        this.resource = resource;
    }
}
