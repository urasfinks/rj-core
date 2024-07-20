package ru.jamsys.core.extension.property;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.item.PropertyInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PropertyValueContainer implements LifeCycleInterface {

    private final Map<String, PropertyValue<?>> map = new ConcurrentHashMap<>();

    public <T> PropertyValue<T> init(
            ApplicationContext applicationContext,
            String ns,
            PropertyInstance<T> propertyInstance,
            BiConsumer<T, T> onUpdate
    ) {
        PropertyValue<?> propertyValue = map.computeIfAbsent(ns, s -> new PropertyValue<>(
                applicationContext,
                s,
                propertyInstance,
                onUpdate
        ));
        @SuppressWarnings("unchecked")
        PropertyValue<T> result = (PropertyValue<T>) propertyValue;
        return result;
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
