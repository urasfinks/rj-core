package ru.jamsys.cache;

import lombok.Getter;
import ru.jamsys.statistic.TimeControllerImpl;

public class CacheItem<T> extends TimeControllerImpl {

    @Getter
    final T value;

    public CacheItem(T value) {
        this.value = value;
    }

}
