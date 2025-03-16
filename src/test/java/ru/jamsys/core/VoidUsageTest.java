package ru.jamsys.core;

import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.item.PropertySubscription;

// IO time: 13ms
// COMPUTE time: 10ms

class VoidUsageTest {

    @SuppressWarnings("all")
    @Test
    void snakeToCamel() {
        try {
            Property property = new Property("", "").setDescriptionIfNull(null);
            PropertySubscription<?> propertySubscription = new ServiceProperty(null).addSubscription(null);
        } catch (Throwable ignore) {
        }
    }


}