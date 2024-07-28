package ru.jamsys.core.extension.property;

import org.springframework.lang.NonNull;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.item.type.PropertyBoolean;
import ru.jamsys.core.extension.property.item.type.PropertyInstance;
import ru.jamsys.core.extension.property.item.type.PropertyInteger;
import ru.jamsys.core.extension.property.item.type.PropertyString;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// Просто позволяет создавать объекты работы с property

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

    public PropertiesNsAgent getNsAgent(
            String ns,
            boolean require,
            @NonNull Consumer<Set<String>> onUpdate
    ) {
        PropertiesRepository propertiesRepository = new PropertiesRepository(null);
        Map<String, String> prop = serviceProperty.getProp();
        UtilRisc.forEach(null, prop, (key, value) -> {
            if (key.startsWith(ns + ".")) {
                propertiesRepository.addProp(key.substring(ns.length() + 1), value);
            }
        });
        return new PropertiesNsAgent(onUpdate::accept, serviceProperty, propertiesRepository, ns, require);
    }

    public PropertiesContainer getContainer() {
        return new PropertiesContainer(serviceProperty);
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

    public <T> Property<T> getProperty(
            String ns,
            Class<T> cls,
            T defValue,
            BiConsumer<T, T> onUpdate // 1: oldValue; 2: newValue
    ) {
        return getProperty(
                ns,
                getPropertyInstance(cls, defValue),
                onUpdate
        );
    }

    public <T> Property<T> getProperty(
            String ns,
            Class<T> cls,
            String defValue,
            BiConsumer<T, T> onUpdate // 1: oldValue; 2: newValue
    ) {
        return getProperty(
                ns,
                getPropertyInstance(cls, defValue),
                onUpdate
        );
    }

    public <X> PropertyInstance<X> getPropertyInstance(Class<X> cls, X value) {
        PropertyInstance<?> propertyInstance = null;
        if (ServiceClassFinder.instanceOf(cls, String.class)) {
            propertyInstance = new PropertyString((String) value);
        } else if (ServiceClassFinder.instanceOf(cls, Boolean.class)) {
            propertyInstance = new PropertyBoolean((Boolean) value);
        } else if (ServiceClassFinder.instanceOf(cls, Integer.class)) {
            propertyInstance = new PropertyInteger((Integer) value);
        }
        @SuppressWarnings("unchecked")
        PropertyInstance<X> resultInstance1 = (PropertyInstance<X>) propertyInstance;
        return resultInstance1;
    }

    public <X> PropertyInstance<X> getPropertyInstance(Class<X> cls, String value) {
        PropertyInstance<?> propertyInstance = null;
        if (ServiceClassFinder.instanceOf(cls, String.class)) {
            propertyInstance = new PropertyString(value);
        } else if (ServiceClassFinder.instanceOf(cls, Boolean.class)) {
            propertyInstance = new PropertyBoolean(value);
        } else if (ServiceClassFinder.instanceOf(cls, Integer.class)) {
            propertyInstance = new PropertyInteger(value);
        }
        @SuppressWarnings("unchecked")
        PropertyInstance<X> resultInstance1 = (PropertyInstance<X>) propertyInstance;
        return resultInstance1;
    }

}
