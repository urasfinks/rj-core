package ru.jamsys.core.extension.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import org.springframework.lang.NonNull;
import ru.jamsys.core.extension.property.item.PropertySubscription;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.flat.util.UtilText;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

// Элемент ServiceProperty, хранит в себе ссылки на подписчиков, кто наблюдает за изменением значений
// При изменении значения рассылает подписчикам уведомления

@JsonPropertyOrder({"key", "keyStructure", "value", "description", "setTrace", "subscriptions"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Property implements PropertyUtil {

    @Getter
    ConcurrentLinkedDeque<SetTrace> setTrace = new ConcurrentLinkedDeque<>(); //key: who, value: value

    @Getter
    private final String key;

    @JsonProperty
    private String value;

    @JsonProperty
    @Getter
    private String description;

    @Getter
    private final Set<PropertySubscription<?>> subscriptions = Util.getConcurrentHashSet();

    public Property(@NonNull String key, String value) {
        this.key = key;
        this.value = value;
        setTrace.add(new SetTrace(value));
    }

    @SuppressWarnings("unused")
    public KeyStructure getKeyStructure() {
        return PropertyUtil.getKeyStructure(key);
    }

    public Property setDescriptionIfNull(String description) {
        if (description == null) {
            return this;
        }
        if (this.description == null) {
            this.description = description;
        }
        return this;
    }

    public void removeSubscription(PropertySubscription<?> propertySubscription) {
        this.subscriptions.remove(propertySubscription);
    }

    // Хранить подписки будем только для одного диспетчера, так как может быть 2 подписки
    // одна попала по regexp а вторая просто по PropertyKey и что бы не было двойных уведомлений
    // будем хранить только один экземпляр подписки.
    private boolean notContainsPropertyDispatcher(PropertySubscription<?> propertySubscription) {
        PropertyDispatcher<?> propertyDispatcher = propertySubscription.getPropertyDispatcher();
        for (PropertySubscription<?> propertySubscription1 : this.subscriptions) {
            if (propertySubscription1.getPropertyDispatcher().equals(propertyDispatcher)) {
                return false;
            }
        }
        return true;
    }

    public void addSubscription(PropertySubscription<?> propertySubscription) {
        String key = propertySubscription.getPropertyKey();
        if (this.key.equals(key) && notContainsPropertyDispatcher(propertySubscription)) {
            this.subscriptions.add(propertySubscription);
        }
        String pattern = propertySubscription.getRegexp();
        if (pattern != null && isMatchPattern(pattern) && notContainsPropertyDispatcher(propertySubscription)) {
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
            setTrace.add(new SetTrace(value));
            if (setTrace.size() > 30) {
                setTrace.removeFirst();
            }
            emit(oldValue);
        }
    }

    public void emit(String oldValue) {
        UtilRisc.forEach(null, subscriptions, subscription -> {
            subscription.getPropertyDispatcher().onPropertyUpdate(getKey(), oldValue, get());
        });
    }

    @JsonIgnore
    public String get() {
        return value;
    }

    public <T> T get(Class<T> cls) {
        @SuppressWarnings("unchecked")
        T apply = (T) PropertyUtil.convertType.get(cls).apply(value);
        return apply;
    }

}
