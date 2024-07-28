package ru.jamsys.core.extension.property;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PropertyNs<T> {

    public static Map<Class<?>, Function<String, ?>> convertType = new HashMap<>() {{
        this.put(String.class, s -> s);
        this.put(Integer.class, Integer::parseInt);
        this.put(Boolean.class, Boolean::parseBoolean);
    }};

    @Getter
    private final String key;

    private final PropertiesNsAgent propertiesNsAgent;

    private final Class<T> cls;

    protected PropertyNs(Class<T> cls, String key, PropertiesNsAgent propertiesNsAgent) {
        this.cls = cls;
        this.key = key;
        this.propertiesNsAgent = propertiesNsAgent;
    }

    public void set(T value) {
        propertiesNsAgent.setProperty(key, value.toString());
    }

    public T get() {
        @SuppressWarnings("unchecked")
        T t = (T) convertType.get(cls).apply(propertiesNsAgent.getWithoutNs(key));
        return t;
    }

}
