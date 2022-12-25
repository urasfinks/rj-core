package ru.jamsys.pool;

import lombok.Data;

import java.util.List;

@Data
public class PoolResource<T> {

    private T resource;
    private long lastRun = System.currentTimeMillis();

    @SuppressWarnings("all")
    public static PoolResource[] toArrayWrapObject(List<PoolResource> l) throws Exception {
        return l.toArray(new PoolResource[0]);
    }

}
