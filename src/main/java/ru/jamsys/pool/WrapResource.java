package ru.jamsys.pool;

import lombok.Data;

import java.util.List;

@Data
public class WrapResource<T> {

    private T resource;
    private long lastRun = System.currentTimeMillis();

    @SuppressWarnings("all")
    public static WrapResource[] toArrayWrapObject(List<WrapResource> l) throws Exception {
        return l.toArray(new WrapResource[0]);
    }

}
