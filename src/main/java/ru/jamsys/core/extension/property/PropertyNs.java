package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.extension.LifeCycleInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PropertyNs<T> implements LifeCycleInterface {

    public static Map<Class<?>, Function<String, ?>> convertType = new HashMap<>() {{
        this.put(String.class, s -> s);
        this.put(Integer.class, Integer::parseInt);
        this.put(Boolean.class, Boolean::parseBoolean);
    }};

    @Getter
    private final String absoluteKey;

    private final PropertiesNsAgent propertiesNsAgent;

    private final Class<T> cls;

    protected PropertyNs(Class<T> cls, String absoluteKey, PropertiesNsAgent propertiesNsAgent) {
        this.cls = cls;
        this.absoluteKey = absoluteKey;
        this.propertiesNsAgent = propertiesNsAgent;
    }

    public void set(T value) {
        propertiesNsAgent.setProperty(absoluteKey, value.toString());
    }

    public T get() {
        @SuppressWarnings("unchecked")
        T t = (T) convertType.get(cls).apply(propertiesNsAgent.getWithoutNs(absoluteKey));
        return t;
    }

    @Override
    public void run() {
        propertiesNsAgent.run();
    }

    @Override
    public void shutdown() {
        propertiesNsAgent.remove(absoluteKey);
    }

}
