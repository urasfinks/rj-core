package ru.jamsys.core.extension.property;

import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

// Хранит в себе ссылки на Property, что бы одним разом можно было от них отписаться
// Никак не работает с NS - только прямые ключи на property

public class PropertiesContainer implements LifeCycleInterface {

    private final Map<String, Property<?>> map = new ConcurrentHashMap<>();

    private final ServiceProperty serviceProperty;

    public PropertiesContainer(ServiceProperty serviceProperty) {
        this.serviceProperty = serviceProperty;
    }

    public Set<String> getKeySet() {
        return map.keySet();
    }

    public <T> Property<T> watch(Class<T> cls, String key, T defValue) {
        return watch(cls, key, defValue, null);
    }

    public <T> Property<T> watch(Class<T> cls, String key, T defValue, BiConsumer<T, T> onUpdate) {
        Property<?> property = map.computeIfAbsent(
                key,
                _ -> serviceProperty.getFactory().getProperty(key, cls, defValue, onUpdate)
        );
        @SuppressWarnings("unchecked")
        Property<T> result = (Property<T>) property;
        return result;
    }

    public void unwatch(String key) {
        Property<?> remove = map.remove(key);
        if (remove != null) {
            remove.shutdown();
        }
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
