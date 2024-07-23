package ru.jamsys.core.extension.property;

import lombok.Setter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.extension.LifeCycleInterface;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PropertiesMap implements LifeCycleInterface {

    private final Map<String, Property<?>> map = new ConcurrentHashMap<>();

    @Setter
    private ApplicationContext applicationContext;

    public <T> Property<T> init(Class<T> cls, String key, T defValue) {
        return init(cls, key, defValue, null);
    }

    public <T> Property<T> init(Class<T> cls, String key, T defValue, BiConsumer<T, T> onUpdate) {
        Property<?> property = map.computeIfAbsent(key, s -> new Property<>(
                applicationContext,
                s,
                PropertyFactory.instanceOf(cls, defValue),
                onUpdate
        ));
        @SuppressWarnings("unchecked")
        Property<T> result = (Property<T>) property;
        return result;
    }

    public <T> Property<T> get(String key) {
        @SuppressWarnings("unchecked")
        Property<T> property = (Property<T>) map.get(key);
        return property;
    }

    @Override
    public void run() {
        map.forEach((_, property) -> property.run());
    }

    @Override
    public void shutdown() {
        map.forEach((_, property) -> property.shutdown());
    }

}
