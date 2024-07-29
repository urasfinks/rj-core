package ru.jamsys.core.extension.property;

import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PropertiesContainer implements LifeCycleInterface {

    private final Map<String, Property<?>> map = new LinkedHashMap<>();

    private final ServiceProperty serviceProperty;

    public PropertiesContainer(ServiceProperty serviceProperty) {
        this.serviceProperty = serviceProperty;
    }

    public <T> Property<T> getProperty(
            Class<T> cls,
            String absoluteKey,
            T defValue,
            boolean required,
            Consumer<T> onUpdate
    ) {
        @SuppressWarnings("unchecked")
        Property<T> tProperty = (Property<T>) map.computeIfAbsent(
                absoluteKey,
                _ -> new Property<>(serviceProperty, cls, absoluteKey, defValue, required, onUpdate)
        );
        return tProperty;
    }

    @Override
    public void run() {
        map.forEach((_, property) -> property.run());
    }

    @Override
    public void shutdown() {
        map.forEach((_, property) -> property.shutdown());
    }

    public void unwatch(String key) {
        Property<?> remove = map.remove(key);
        if (remove != null) {
            remove.shutdown();
        }
    }
}
