package ru.jamsys.core.extension.property;

import lombok.Setter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.item.PropertyInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PropertyValueContainer implements LifeCycleInterface {

    private final Map<String, PropertyValue<?>> map = new ConcurrentHashMap<>();

    @Setter
    private ApplicationContext applicationContext;

    public <T> PropertyValue<T> init(Class<T> cls, String key, T defValue) {
        return init(cls, key, defValue, null);
    }

    public <T> PropertyValue<T> init(Class<T> cls, String key, T defValue, BiConsumer<T, T> onUpdate) {
        PropertyValue<?> propertyValue = map.computeIfAbsent(key, s -> new PropertyValue<>(
                applicationContext,
                s,
                PropertyInstance.instanceOf(cls, defValue),
                onUpdate
        ));
        @SuppressWarnings("unchecked")
        PropertyValue<T> result = (PropertyValue<T>) propertyValue;
        return result;
    }

    public <T> PropertyValue<T> get(String key) {
        @SuppressWarnings("unchecked")
        PropertyValue<T> propertyValue = (PropertyValue<T>) map.get(key);
        return propertyValue;
    }

    @Override
    public void run() {
        map.forEach((_, propertyValue) -> propertyValue.run());
    }

    @Override
    public void shutdown() {
        map.forEach((_, propertyValue) -> propertyValue.shutdown());
    }

}
