package ru.jamsys.core.extension.property;

import ru.jamsys.core.component.ServiceProperty;

import java.util.function.Consumer;

// Просто позволяет создавать объекты работы с property

public class ServicePropertyFactory {

    private final ServiceProperty serviceProperty;

    public ServicePropertyFactory(ServiceProperty serviceProperty) {
        this.serviceProperty = serviceProperty;
    }

    // Получить свойство
    public <T> PropertyNs<T> getPropertyNs(
            Class<T> cls,
            String absoluteKey,
            boolean required,
            T defValue,
            Consumer<T> onUpdate
    ) {
        return new PropertyNs<>(serviceProperty, cls, absoluteKey, defValue, required, onUpdate);
    }

    public PropertiesAgent getPropertiesAgent(
            PropertyUpdateDelegate subscriber,
            PropertiesRepository propertiesRepository,
            String ns,
            boolean require
    ) {
        return new PropertiesAgent(serviceProperty, subscriber, propertiesRepository, ns, require);
    }


    public PropertiesContainer getContainer() {
        return new PropertiesContainer(serviceProperty);
    }

}
