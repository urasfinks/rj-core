package ru.jamsys.core.extension.property;

import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.repository.PropertiesRepository;
import ru.jamsys.core.extension.property.repository.PropertiesRepositoryMap;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.function.Consumer;

// Просто позволяет создавать объекты работы с Property

public class ServicePropertyFactory {

    private final ServiceProperty serviceProperty;

    public ServicePropertyFactory(ServiceProperty serviceProperty) {
        this.serviceProperty = serviceProperty;
    }

    // Получить свойство
    public <T> Property<T> getProperty(
            Class<T> cls,
            String absoluteKey,
            T defValue,
            boolean required,
            Consumer<T> onUpdate
    ) {
        return new Property<>(serviceProperty, cls, absoluteKey, defValue, required, onUpdate);
    }

    // Загружает ключи через PropertiesRepository
    public PropertiesAgent getPropertiesAgent(
            PropertyUpdateDelegate subscriber,
            PropertiesRepository propertiesRepository,
            String ns,
            boolean require
    ) {
        return new PropertiesAgent(serviceProperty, subscriber, propertiesRepository, ns, require);
    }

    //Загружает все ключи из Properties на основе propertiesRepositoryMap
    public PropertiesAgent getPropertiesAgentMap(
            PropertyUpdateDelegate subscriber,
            String ns,
            boolean require
    ) {
        PropertiesRepositoryMap propertiesRepositoryMap = new PropertiesRepositoryMap();
        UtilRisc.forEach(null, serviceProperty.getProp(), (key, value) -> {
            if (key.startsWith(ns + ".")) {
                propertiesRepositoryMap.getPropValue().put(key.substring(ns.length() + 1), value);
            }
        });
        return new PropertiesAgent(serviceProperty, subscriber, propertiesRepositoryMap, ns, require);
    }

    // Контейнер Property
    public PropertiesContainer getContainer() {
        return new PropertiesContainer(serviceProperty);
    }

}
