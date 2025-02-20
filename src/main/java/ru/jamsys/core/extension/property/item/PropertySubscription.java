package ru.jamsys.core.extension.property.item;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.PropertySubscriber;

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

    private String keyPattern; //regexp

    private String key;

    @ToString.Exclude
    private final PropertySubscriber propertySubscriber;

    public PropertySubscription(PropertySubscriber propertySubscriber) {
        this.propertySubscriber = propertySubscriber;
    }

    public void onPropertyUpdate(Property property){
        propertySubscriber.onPropertySubscriptionUpdate(property);
    }

}
