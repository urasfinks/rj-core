package ru.jamsys.core.extension.property;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class PropertyEnvelope<T, K, V> implements Property<K, V> {

    final T value;

    Map<K, V> mapProperty = new HashMap<>();

    public PropertyEnvelope(T value) {
        this.value = value;
    }

}
