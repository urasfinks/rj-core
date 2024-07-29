package ru.jamsys.core.extension.property;

import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PropertiesContainer implements LifeCycleInterface {

    private final Map<String, PropertyNs<?>> map = new LinkedHashMap<>();

    private final ServiceProperty serviceProperty;

    public PropertiesContainer(ServiceProperty serviceProperty) {
        this.serviceProperty = serviceProperty;
    }

    public <T> PropertyNs<T> getPropertyNs(
            Class<T> cls,
            String absoluteKey,
            T defValue,
            boolean required,
            Consumer<T> onUpdate
    ) {
        @SuppressWarnings("unchecked")
        PropertyNs<T> tPropertyNs = (PropertyNs<T>) map.computeIfAbsent(
                absoluteKey,
                _ -> new PropertyNs<>(serviceProperty, cls, absoluteKey, defValue, required, onUpdate)
        );
        return tPropertyNs;
    }

    @Override
    public void run() {
        map.forEach((_, propertyNs) -> propertyNs.run());
    }

    @Override
    public void shutdown() {
        map.forEach((_, propertyNs) -> propertyNs.shutdown());
    }
}
