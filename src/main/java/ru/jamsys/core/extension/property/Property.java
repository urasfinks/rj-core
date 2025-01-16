package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.item.PropertySubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

// Привязанный объект к ServiceProperty, хранит актуальное значение + имеет возможность хуков обновления

public class Property<T> implements PropertyUpdateDelegate, LifeCycleInterface {

    public static Map<Class<?>, Function<String, ?>> convertType = new HashMap<>() {{
        this.put(String.class, s -> s);
        this.put(Integer.class, Integer::parseInt);
        this.put(Boolean.class, Boolean::parseBoolean);
    }};

    @Getter
    private final String propKey;

    private final ServiceProperty serviceProperty;

    private final Class<T> cls;

    @Getter
    private final T defValue;

    @Getter
    private T value;

    @Getter
    private final boolean required;

    private final Consumer<T> onUpdate;

    private PropertySubscriber propertySubscriber;

    public Property(
            ServiceProperty serviceProperty,
            Class<T> cls,
            String propKey,
            T defValue,
            boolean required,
            Consumer<T> onUpdate
    ) {
        this.onUpdate = onUpdate;
        this.serviceProperty = serviceProperty;
        this.cls = cls;
        this.propKey = propKey;
        this.defValue = defValue;
        this.required = required;
        run();
    }

    public void set(T value) {
        this.value = value;
        serviceProperty.setProperty(propKey, value.toString());
    }

    public T get() {
        return this.value;
    }

    @Override
    public void onPropertyUpdate(Map<String, String> mapAlias) {
        @SuppressWarnings("unchecked")
        T t = (T) convertType.get(cls).apply(mapAlias.getOrDefault(propKey, String.valueOf(defValue)));
        this.value = t;
        if (this.onUpdate != null) {
            this.onUpdate.accept(this.value);
        }
    }

    @Override
    public void run() {
        propertySubscriber = this.serviceProperty.subscribe(propKey, this, required, String.valueOf(defValue));
    }

    @Override
    public void shutdown() {
        if(propertySubscriber != null){
            serviceProperty.unsubscribe(propertySubscriber);
        }
    }

}
