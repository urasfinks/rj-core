package ru.jamsys.core.extension.property;

import lombok.Setter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.item.PropertyBoolean;
import ru.jamsys.core.extension.property.item.PropertyInstance;
import ru.jamsys.core.extension.property.item.PropertyInteger;
import ru.jamsys.core.extension.property.item.PropertyString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PropertyValueContainer implements LifeCycleInterface {

    private final Map<String, PropertyValue<?>> map = new ConcurrentHashMap<>();

    @Setter
    private ApplicationContext applicationContext;

    public PropertyValue<Integer> initInt(
            String ns,
            Integer defValue,
            BiConsumer<Integer, Integer> onUpdate
    ) {
        PropertyValue<?> propertyValue = map.computeIfAbsent(ns, s -> new PropertyValue<>(
                applicationContext,
                s,
                new PropertyInteger(defValue),
                onUpdate
        ));
        @SuppressWarnings("unchecked")
        PropertyValue<Integer> result = (PropertyValue<Integer>) propertyValue;
        return result;
    }

    public PropertyValue<Boolean> initBoolean(
            String ns,
            Boolean defValue,
            BiConsumer<Boolean, Boolean> onUpdate
    ) {
        PropertyValue<?> propertyValue = map.computeIfAbsent(ns, s -> new PropertyValue<>(
                applicationContext,
                s,
                new PropertyBoolean(defValue),
                onUpdate
        ));
        @SuppressWarnings("unchecked")
        PropertyValue<Boolean> result = (PropertyValue<Boolean>) propertyValue;
        return result;
    }

    public PropertyValue<String> initString(
            String ns,
            String defValue,
            BiConsumer<String, String> onUpdate
    ) {
        PropertyValue<?> propertyValue = map.computeIfAbsent(ns, s -> new PropertyValue<>(
                applicationContext,
                s,
                new PropertyString(defValue),
                onUpdate
        ));
        @SuppressWarnings("unchecked")
        PropertyValue<String> result = (PropertyValue<String>) propertyValue;
        return result;
    }

    public <T> PropertyValue<T> init(
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
