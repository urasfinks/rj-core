package ru.jamsys.core.extension.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.item.PropertySubscription;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.flat.util.UtilText;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;

// Элемент ServiceProperty, хранит в себе ссылки на подписчиков, кто наблюдает за изменением значений
// При изменении значения рассылает подписчикам уведомления

@JsonPropertyOrder({"key", "value", "description", "setTrace", "subscriptions"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Property {

    public static Map<Class<?>, Function<String, ?>> convertType = new HashMap<>() {{
        this.put(String.class, s -> s);
        this.put(Integer.class, Integer::parseInt);
        this.put(Boolean.class, Boolean::parseBoolean);
    }};

    @JsonPropertyOrder({"stack", "value", "time"})
    @Getter
    public static class Trace {

        private String stack;

        private final String value;

        private final String time;

        @Setter
        private String resource;

        public Trace(String value) {
            this.value = value;
            this.time = UtilDate.get(UtilDate.format);
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

            int idx = -1;
            for (StackTraceElement stackTraceElement : stackTrace) {
                idx++;
                if (idx < 1) {
                    continue;
                }
                if (stackTraceElement.getFileName() == null
                        || stackTraceElement.getFileName().equals(ConcurrentHashMap.class.getSimpleName() + ".java")
                        || stackTraceElement.getFileName().equals(PropertyDispatcher.class.getSimpleName() + ".java")
                        || stackTraceElement.getFileName().equals(PropertySubscription.class.getSimpleName() + ".java")
                        || stackTraceElement.getFileName().equals(ServiceProperty.class.getSimpleName() + ".java")
                        || stackTraceElement.getFileName().equals(Property.class.getSimpleName() + ".java")
                        || stackTraceElement.getFileName().equals(UtilRisc.class.getSimpleName() + ".java")
                ) {
                    continue;
                }
                if (stackTraceElement.getClassName().startsWith(ServiceClassFinder.pkg)) {
                    this.stack = ExceptionHandler.getLineStack(stackTraceElement);
                    break;
                }
            }
        }

    }

    @Getter
    ConcurrentLinkedDeque<Trace> setTrace = new ConcurrentLinkedDeque<>(); //key: who, value: value

    @Getter
    private final String key;

    @JsonProperty
    private String value;

    @JsonProperty
    private String description;

    @Getter
    private final Set<PropertySubscription> subscriptions = Util.getConcurrentHashSet();

    public Property(@NonNull String key, String value) {
        this.key = key;
        this.value = value;
        setTrace.add(new Trace(value));
    }

    public Property setDescriptionIfNull(String description) {
        if (this.description == null) {
            this.description = description;
        }
        return this;
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
        return UtilText.regexpFind(this.key, regexp) != null;
    }

    public void set(Object newValue) {
        set(newValue == null ? null : String.valueOf(newValue));
    }

    public void set(String newValue) {
        String oldValue = value;
        if (!Objects.equals(value, newValue)) {
            this.value = newValue;
            setTrace.add(new Trace(value));
            if (setTrace.size() > 30) {
                setTrace.removeFirst();
            }
            emit(oldValue);
        }
    }

    public void emit(String oldValue) {
        UtilRisc.forEach(null, subscriptions, subscription -> {
            subscription.onPropertyUpdate(oldValue, this);
        });
    }

    @JsonIgnore
    public String get() {
        return value;
    }

    public <T> T get(Class<T> cls) {
        @SuppressWarnings("unchecked")
        T apply = (T) convertType.get(cls).apply(value);
        return apply;
    }

}
