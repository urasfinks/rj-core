package ru.jamsys.core.extension.property;

import lombok.Getter;
import org.springframework.lang.NonNull;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.item.PropertySubscription;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;

// Элемент ServiceProperty, хранит в себе ссылки на подписчиков, кто наблюдает за изменением значений
// При изменении значения рассылает подписчикам уведомления

public class Property {

    public static Map<Class<?>, Function<String, ?>> convertType = new HashMap<>() {{
        this.put(String.class, s -> s);
        this.put(Integer.class, Integer::parseInt);
        this.put(Boolean.class, Boolean::parseBoolean);
    }};

    @Getter
    ConcurrentLinkedDeque<Map<String, String>> setup = new ConcurrentLinkedDeque<>(); //key: who, value: value

    @Getter
    private final String key;

    private String value;

    @Getter
    private final Set<PropertySubscription> subscriptions = new LinkedHashSet<>();

    public Property(@NonNull String key, String value, String who) {
        this.key = key;
        this.value = value;
        setup.add(new HashMapBuilder<String, String>().append(who, value));
    }

    public void removeSubscription(PropertySubscription propertySubscription) {
        this.subscriptions.remove(propertySubscription);
    }

    public void addSubscription(PropertySubscription propertySubscription) {
        String key = propertySubscription.getKey();
        if (this.key.equals(key)) {
            this.subscriptions.add(propertySubscription);
        }
        String pattern = propertySubscription.getKeyPattern();
        if (pattern != null && Util.regexpFind(this.key, pattern) != null) {
            this.subscriptions.add(propertySubscription);
        }
    }

    public void set(String newValue, String who) {
        if (!Objects.equals(value, newValue)) {
            this.value = newValue;
            setup.add(new HashMapBuilder<String, String>().append(who, value));
            if (setup.size() > 30) {
                setup.removeFirst();
            }
            UtilRisc.forEach(null, subscriptions, subscription -> {
                subscription.onPropertyUpdate(this);
            });
        }
    }

    public void set(Object newValue, String who) {
        set(newValue == null ? null : String.valueOf(newValue), who);
    }

    public String get() {
        return value;
    }

    public <T> T get(Class<T> cls) {
        @SuppressWarnings("unchecked")
        T apply = (T) convertType.get(cls).apply(value);
        return apply;
    }

}
