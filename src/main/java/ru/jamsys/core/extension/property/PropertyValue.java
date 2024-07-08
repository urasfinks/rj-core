package ru.jamsys.core.extension.property;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.component.ServiceProperty;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Главное не забывать закрывать после использования

public class PropertyValue extends PropertyConnector implements PropertySubscriberNotify {

    private AtomicInteger intValue = null;

    private AtomicBoolean boolValue = null;

    @SuppressWarnings("all")
    @PropertyName
    private String prop;

    @Getter
    private final String ns;

    private final Subscriber subscriber;

    private final PropertyType propertyType;

    public PropertyValue(
            ApplicationContext applicationContext,
            PropertyType propertyType,
            String ns,
            String defValue
    ) {
        this.propertyType = propertyType;
        switch (propertyType) {
            case INTEGER -> intValue = new AtomicInteger(Integer.parseInt(defValue));
            case BOOLEAN -> boolValue = new AtomicBoolean(Boolean.parseBoolean(defValue));
        }
        this.prop = defValue;
        this.ns = ns;
        subscriber = applicationContext.getBean(ServiceProperty.class).getSubscriber(
                this,
                this,
                ns,
                false
        );
    }

    public int getAsInt() {
        return intValue.get();
    }

    public boolean getAsBool() {
        return boolValue.get();
    }

    public String getAsString() {
        return prop;
    }

    public void set(String value) {
        subscriber.setProperty("", value);
    }

    public void set(int value) {
        subscriber.setProperty("", value+"");
    }

    public void set(boolean value) {
        subscriber.setProperty("", value ? "true" : "false");
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedProp) {
        switch (propertyType) {
            case INTEGER -> intValue.set(Integer.parseInt(prop));
            case BOOLEAN -> boolValue.set(Boolean.parseBoolean(prop));
        }
    }

    public void close() {
        subscriber.unsubscribe();
    }

}
