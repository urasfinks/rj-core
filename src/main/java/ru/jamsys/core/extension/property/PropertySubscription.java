package ru.jamsys.core.extension.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.property.repository.AbstractRepositoryProperty;

import java.util.Objects;

// Подписка - это как НСИ, что ключ (PropertyKey) или шаблон (regexp) привязан к подписчику (PropertyDispatcher).
// Подписка хранится в Property, для того, что бы когда будет изменение значения
// пробежаться по всем подпискам и вызвать onUpdate, что бы до подписчиков (диспетчеров) распространить событие изменения.
// Так же подписка хранится в подписчике (PropertyDispatcher), что бы иметь возможность отписаться или заново подписаться
// run() / shutdown()

@Getter
@ToString
@Setter
@Accessors(chain = true)
public class PropertySubscription<T> {

    private String regexp; //regexp

    private String propertyKey;

    @ToString.Exclude
    @JsonIgnore
    @Getter
    private final PropertyDispatcher<T> propertyDispatcher;

    public PropertySubscription(PropertyDispatcher<T> propertyDispatcher) {
        this.propertyDispatcher = propertyDispatcher;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertySubscription<?> that = (PropertySubscription<?>) o;
        return Objects.equals(regexp, that.regexp)
                && Objects.equals(propertyKey, that.propertyKey)
                && Objects.equals(propertyDispatcher, that.propertyDispatcher);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regexp, propertyKey, propertyDispatcher);
    }

    @SuppressWarnings("unused") //used UtilJson
    public String getClassPropertyListener() {
        PropertyListener propertyListener = propertyDispatcher.getPropertyListener();
        if (propertyListener != null) {
            return propertyListener.getClass().getName();
        }
        return null;
    }

    @SuppressWarnings("unused") //used UtilJson
    public String getClassPropertyRepository() {
        AbstractRepositoryProperty<T> repositoryProperty = propertyDispatcher.getRepositoryProperty();
        if (repositoryProperty != null) {
            return repositoryProperty.getClass().getName();
        }
        return null;
    }

}
