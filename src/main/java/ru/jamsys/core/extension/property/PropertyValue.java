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

public class PropertyValue<T> extends PropertyConnector implements PropertySubscriberNotify, LifeCycleInterface {

    private final PropertyInstance<T> value;

    @SuppressWarnings("all")
    @PropertyName
    private String prop;

    @Getter
    private final String ns;

    private final Subscriber subscriber;

    private final BiConsumer<T, T> onUpdate;

    public PropertyValue(
            ApplicationContext applicationContext,
            String ns,
            PropertyInstance<T> value,
            BiConsumer<T, T> onUpdate // 1: oldValue; 2: newValue
    ) {
        this.onUpdate = onUpdate;
        this.value = value;
        this.prop = value.getAsString();
        this.ns = ns;
        subscriber = applicationContext.getBean(ServiceProperty.class).getSubscriber(
                this,
                this,
                ns,
                false
        );
    }

    public void set(String value) {
        subscriber.setProperty("", value);
    }

    public void set(int value) {
        subscriber.setProperty("", value + "");
    }

    public void set(boolean value) {
        subscriber.setProperty("", value ? "true" : "false");
    }

    public T get() {
        return value.get();
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedPropAlias) {
        T oldValue = get();
        value.set(prop);
        if (onUpdate != null) {
            onUpdate.accept(oldValue, get());
        }
    }

    @Override
    public void run() {
        subscriber.run();
    }

    @Override
    public void shutdown() {
        subscriber.shutdown();
    }

}
