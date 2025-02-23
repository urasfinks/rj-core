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
        if(key.equals("App.ManagerRateLimit.RateLimit.App.ManagerThreadPool.ThreadPool.seq.then1.tps")){
            Util.printStackTrace("!!");
        }
        setup.add(new HashMapBuilder<String, String>().append(who, value));
    }

    public void removeSubscription(PropertySubscription propertySubscription) {
        this.subscriptions.remove(propertySubscription);
    }

    public void addSubscription(PropertySubscription propertySubscription) {
        String key = propertySubscription.getPropertyKey();
        if (this.key.equals(key)) {
            this.subscriptions.add(propertySubscription);
        }
        String pattern = propertySubscription.getRegexp();
        if (pattern != null && isMatchPattern(pattern)) {
            this.subscriptions.add(propertySubscription);
        }
    }

    // Ключ подходит по шаблону
    public boolean isMatchPattern(String regexp) {
        return Util.regexpFind(this.key, regexp) != null;
    }

    public void set(String newValue, String who) {
        String oldValue = value;
        if (!Objects.equals(value, newValue)) {
            this.value = newValue;
            setup.add(new HashMapBuilder<String, String>().append(who, value));
            if (setup.size() > 30) {
                setup.removeFirst();
            }
            emit(oldValue);
        }
    }

    public void emit(String oldValue) {
        //System.out.println("emit:  " + getKey());
        UtilRisc.forEach(null, subscriptions, subscription -> {
            subscription.onPropertyUpdate(oldValue, this);
        });
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
