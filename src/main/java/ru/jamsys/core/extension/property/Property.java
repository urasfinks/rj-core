package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;

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
    private final String absoluteKey;

    private final ServiceProperty serviceProperty;

    private final Class<T> cls;

    private final T defValue;

    private T value;

    private final boolean required;

    private final Consumer<T> onUpdate;

    private PropertyFollower follower;

    public Property(
            ServiceProperty serviceProperty,
            Class<T> cls,
            String absoluteKey,
            T defValue,
            boolean required,
            Consumer<T> onUpdate
    ) {
        this.onUpdate = onUpdate;
        this.serviceProperty = serviceProperty;
        this.cls = cls;
        this.absoluteKey = absoluteKey;
        this.defValue = defValue;
        this.required = required;
        run();
    }

    public void set(T value) {
        this.value = value;
        serviceProperty.setProperty(absoluteKey, value.toString());
    }

    public T get() {
        return this.value;
    }

    @Override
    public void onPropertyUpdate(Map<String, String> mapAlias) {
        @SuppressWarnings("unchecked")
        T t = (T) convertType.get(cls).apply(mapAlias.getOrDefault(absoluteKey, String.valueOf(defValue)));
        this.value = t;
        if (this.onUpdate != null) {
            this.onUpdate.accept(this.value);
        }
    }

    @Override
    public void run() {
        follower = this.serviceProperty.subscribe(absoluteKey, this, required, String.valueOf(defValue));
    }

    @Override
    public void shutdown() {
        if(follower != null){
            serviceProperty.unsubscribe(follower);
        }
    }

}
