package ru.jamsys.core.extension.property.item;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.PropertySubscriber;
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

    private String propertyKey;

    private String defaultValue;

    @ToString.Exclude
    private final PropertySubscriber propertySubscriber;

    @ToString.Exclude
    private final ServiceProperty serviceProperty;

    public PropertySubscription(PropertySubscriber propertySubscriber, ServiceProperty serviceProperty) {
        this.propertySubscriber = propertySubscriber;
        this.serviceProperty = serviceProperty;
    }

    public void onPropertyUpdate(Property property) {
        propertySubscriber.onPropertySubscriptionUpdate(property);
    }

    // Пролить значения до PropertyRepository
    public PropertySubscription syncPropertyRepository(String who) {
        if (propertyKey != null) {
            // Получили default значение, получили Property, если не сошлись, считаем приоритетным Property.get()
            String propertyValue = serviceProperty.computeIfAbsent(propertyKey, defaultValue, who).get();
            if (!Objects.equals(defaultValue, propertyValue)) {
                propertySubscriber.setRepositoryProxy(propertyKey, propertyValue);
            }
        }
        if (regexp != null) {
            UtilRisc.forEach(null, serviceProperty.get(regexp), property -> {
                propertySubscriber.setRepositoryProxy(property.getKey(), property.get());
            });
        }
        return this;
    }

}
