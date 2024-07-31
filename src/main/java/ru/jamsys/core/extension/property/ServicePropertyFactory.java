package ru.jamsys.core.extension.property;

import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.repository.PropertiesRepository;
import ru.jamsys.core.extension.property.repository.PropertiesRepositoryMap;

import java.util.function.Consumer;

// Просто позволяет создавать объекты работы с Property

public class ServicePropertyFactory {

    private final ServiceProperty serviceProperty;

    public ServicePropertyFactory(ServiceProperty serviceProperty) {
        this.serviceProperty = serviceProperty;
    }

    // Получить свойство, в котором значение будет синхронизированно с ServiceProperty
    public <T> Property<T> getProperty(
            Class<T> cls,
            String propKey,
            T defValue,
            boolean required,
            Consumer<T> onUpdate
    ) {
        return new Property<>(serviceProperty, cls, propKey, defValue, required, onUpdate);
    }

    // Загружает ключи через PropertiesRepository
    public PropertiesAgent getPropertiesAgentField(
            PropertyUpdateDelegate subscriber,
            PropertiesRepository propertiesRepository,
            String ns,
            boolean require
    ) {
        return new PropertiesAgent(serviceProperty, subscriber, propertiesRepository, ns, require);
    }

    // Отдаёт PropertiesAgent на основе propertiesRepositoryMap, это даёт возможность загружать произвольные propKey
    // и наблюдать актуальные значения в PropertiesRepository
    public PropertiesAgent getPropertiesAgentMap(
            PropertyUpdateDelegate subscriber,
            String ns,
            boolean require
    ) {
        PropertiesRepositoryMap propertiesRepositoryMap = new PropertiesRepositoryMap();
        return new PropertiesAgent(serviceProperty, subscriber, propertiesRepositoryMap, ns, require);
    }

    // Контейнер Property
    public PropertiesContainer getContainer() {
        return new PropertiesContainer(serviceProperty);
    }

}
