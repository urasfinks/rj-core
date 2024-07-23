package ru.jamsys.core.extension.property;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.item.PropertyInstance;

import java.util.Set;
import java.util.function.BiConsumer;

// Главное не забывать закрывать после использования

public class PropertyValue<T> extends PropertyConnector implements PropertyUpdateNotifier, LifeCycleInterface {

    private final PropertyInstance<T> propertyInstance;

    @SuppressWarnings("all")
    @PropertyName
    private String prop;

    @Getter
    private final String ns;

    private final NameSpaceAgent nameSpaceAgent;

    private final BiConsumer<T, T> onUpdate;

    public PropertyValue(
            ApplicationContext applicationContext,
            String ns,
            PropertyInstance<T> propertyInstance,
            BiConsumer<T, T> onUpdate // 1: oldValue; 2: newValue
    ) {
        this.onUpdate = onUpdate;
        this.propertyInstance = propertyInstance;
        this.prop = propertyInstance.getAsString();
        this.ns = ns;
        nameSpaceAgent = applicationContext.getBean(ServiceProperty.class).getSubscriber(
                this,
                this,
                ns,
                false
        );
    }

    public void set(String value) {
        nameSpaceAgent.setProperty("", value);
    }

    public void set(int value) {
        nameSpaceAgent.setProperty("", value + "");
    }

    public void set(boolean value) {
        nameSpaceAgent.setProperty("", value ? "true" : "false");
    }

    public T get() {
        return propertyInstance.get();
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedPropAlias) {
        T oldValue = get();
        propertyInstance.set(prop);
        if (onUpdate != null) {
            onUpdate.accept(oldValue, get());
        }
    }

    @Override
    public void run() {
        nameSpaceAgent.run();
    }

    @Override
    public void shutdown() {
        nameSpaceAgent.shutdown();
    }

}
