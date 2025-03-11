package ru.jamsys.core.extension.property.item;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.extension.property.repository.PropertyRepository;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.Objects;

// Подписка - это как НСИ, что ключ или шаблон property привязан к подписчику
// Подписка хранится в Property, для того, что бы когда будет изменение значения
// пробежаться по всем подпискам и вызвать onUpdate, что бы до подписчиков распространить событие изменения
// Так же подписка хранится в подписчике, у подписчика список по подпискам, так как подписчик может подписываться сразу
// на несколько полей

@Getter
@ToString
@Setter
@Accessors(chain = true)
public class PropertySubscription {

    private String regexp; //regexp

    @JsonIgnore
    private String propertyKey;

    private String defaultValue;

    @ToString.Exclude
    @JsonIgnore
    private final PropertyDispatcher propertyDispatcher;

    @SuppressWarnings("unused") //used UtilJson
    public String getSubscriberNamespace() {
        return propertyDispatcher.getNamespace();
    }

    @SuppressWarnings("unused") //used UtilJson
    public String getUpdaterClass() {
        PropertyListener propertyListener = propertyDispatcher.getPropertyListener();
        if (propertyListener != null) {
            return propertyListener.getClass().getName();
        }
        return null;
    }

    @SuppressWarnings("unused") //used UtilJson
    public String getPropertyRepositoryClass() {
        PropertyRepository propertyRepository = propertyDispatcher.getPropertyRepository();
        if (propertyRepository != null) {
            return propertyRepository.getClass().getName();
        }
        return null;
    }

    @ToString.Exclude
    @JsonIgnore
    private final ServiceProperty serviceProperty;

    public PropertySubscription(PropertyDispatcher propertyDispatcher, ServiceProperty serviceProperty) {
        this.propertyDispatcher = propertyDispatcher;
        this.serviceProperty = serviceProperty;
    }

    // Вызывается из Property, когда обновлено значение
    public void onPropertyUpdate(String oldValue, Property property) {
        propertyDispatcher.onPropertySubscriptionUpdate(oldValue, property);
    }

    // Пролить значения до PropertyRepository
    public PropertySubscription syncPropertyRepository() {
        if (propertyKey != null) {
            // Получили default значение, получили Property, если не сошлись, считаем приоритетным Property.get()
            String propertyValue = serviceProperty.computeIfAbsent(propertyKey, defaultValue, property -> {
                PropertyRepository propertyRepository = propertyDispatcher.getPropertyRepository();
                if (propertyRepository != null) {
                    property.getSetTrace().getLast().setResource(propertyRepository.getClass().getName());
                }
            }).get();
            if (!Objects.equals(defaultValue, propertyValue)) {
                propertyDispatcher.setRepositoryProxy(propertyKey, propertyValue);
            }
        }
        if (regexp != null) {
            UtilRisc.forEach(null, serviceProperty.get(regexp), property -> {
                propertyDispatcher.setRepositoryProxy(property.getKey(), property.get());
            });
        }
        return this;
    }

}
