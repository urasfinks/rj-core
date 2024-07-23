package ru.jamsys.core.extension.property;

import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.item.type.PropertyInstance;

import java.util.function.BiConsumer;

public class ServicePropertyFactory {

    private final ServiceProperty serviceProperty;

    public ServicePropertyFactory(ServiceProperty serviceProperty) {
        this.serviceProperty = serviceProperty;
    }

    public PropertiesNsAgent getNsAgent(
            PropertyUpdateDelegate propertyUpdateDelegate,
            PropertiesRepository propertiesRepository
    ) {
        return getNsAgent(propertyUpdateDelegate, propertiesRepository, null, true);
    }

    public PropertiesNsAgent getNsAgent(
            PropertyUpdateDelegate propertyUpdateDelegate,
            PropertiesRepository propertiesRepository,
            String ns
    ) {
        return new PropertiesNsAgent(propertyUpdateDelegate, serviceProperty, propertiesRepository, ns, true);
    }

    public PropertiesNsAgent getNsAgent(
            PropertyUpdateDelegate propertyUpdateDelegate,
            PropertiesRepository propertiesRepository,
            String ns,
            boolean require
    ) {
        return new PropertiesNsAgent(propertyUpdateDelegate, serviceProperty, propertiesRepository, ns, require);
    }

    public PropertiesMap getMap() {
        return new PropertiesMap(serviceProperty);
    }

    public <T> Property<T> getProperty(
            String ns,
            PropertyInstance<T> propertyInstance,
            BiConsumer<T, T> onUpdate // 1: oldValue; 2: newValue
    ) {
        return new Property<>(
                serviceProperty,
                ns,
                propertyInstance,
                onUpdate
        );
    }

}
