package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.item.type.PropertyInstance;

import java.util.Set;
import java.util.function.BiConsumer;

// Главное не забывать закрывать после использования

public class Property<T> extends PropertiesRepository implements PropertyUpdateDelegate, LifeCycleInterface {

    private final PropertyInstance<T> propertyInstance;

    @SuppressWarnings("all")
    @PropertyName
    private String value;

    @Getter
    private final String ns;

    private final PropertiesNsAgent propertiesNsAgent;

    private final BiConsumer<T, T> onUpdate;

    public Property(
            ServiceProperty serviceProperty,
            String ns,
            PropertyInstance<T> propertyInstance,
            BiConsumer<T, T> onUpdate // 1: oldValue; 2: newValue
    ) {
        this.onUpdate = onUpdate;
        this.propertyInstance = propertyInstance;
        this.value = propertyInstance.getAsString();
        this.ns = ns;
        propertiesNsAgent = serviceProperty.getFactory().getNsAgent(
                this,
                this,
                ns,
                false
        );
    }

    public void set(T value) {
        propertiesNsAgent.setProperty("", value.toString());
    }

    public T get() {
        return propertyInstance.get();
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedPropAlias) {
        T oldValue = get();
        propertyInstance.set(value);
        if (onUpdate != null) {
            onUpdate.accept(oldValue, get());
        }
    }

    @Override
    public void run() {
        propertiesNsAgent.run();
    }

    @Override
    public void shutdown() {
        propertiesNsAgent.shutdown();
    }

}
