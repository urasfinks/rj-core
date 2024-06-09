package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.PropertyComponent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class Subscriber {

    private final PropertySubscriberNotify subscriber;

    private final PropertyComponent component;

    private final Set<String> subscriptions = new HashSet<>();

    private final PropertyConnector propertyConnector;

    private final String ns;

    public Subscriber(PropertySubscriberNotify subscriber, PropertyComponent component, PropertyConnector propertyConnector, String ns) {
        this.subscriber = subscriber;
        this.component = component;
        this.propertyConnector = propertyConnector;
        this.ns = ns;
        init(true);
    }

    public Subscriber(PropertySubscriberNotify subscriber, PropertyComponent component, PropertyConnector propertyConnector, String ns, boolean require) {
        this.subscriber = subscriber;
        this.component = component;
        this.propertyConnector = propertyConnector;
        this.ns = ns;
        init(require);
    }

    public void setProperty(String key, String value) {
        this.component.setProperty(getKeyWithNamespace(key), value);
    }

    public void init(boolean require) {
        Map<String, String> mapPropValue = this.propertyConnector.getMapPropValue();
        for (String key : mapPropValue.keySet()) {
            subscribe(key, mapPropValue.get(key), require);
        }
    }

    private String getKeyWithNamespace(String key) {
        return ns != null ? (ns + "." + key) : key;
    }

    public Subscriber subscribe(String key, boolean require) {
        return subscribe(key, null, require);
    }

    public Subscriber subscribe(String key, String defValue, boolean require) {
        if (!subscriptions.contains(key)) {
            subscriptions.add(key);
            component.subscribe(getKeyWithNamespace(key), this, require, defValue);
        }
        return this;
    }

    public void unsubscribe(String key) {
        subscriptions.remove(key);
        component.unsubscribe(getKeyWithNamespace(key), this);
    }

    public void unsubscribe() {
        for (String key : subscriptions) {
            component.unsubscribe(getKeyWithNamespace(key), this);
        }
        subscriptions.clear();
    }

    public void onUpdate(Map<String, String> map) {
        Set<String> updatedProp = new HashSet<>();
        if (ns != null) {
            for (String key : map.keySet()) {
                String prop = key.substring(ns.length() + 1);
                updatedProp.add(prop);
                propertyConnector.setValueByProp(prop, map.get(key));
            }

        } else {
            for (String key : map.keySet()) {
                updatedProp.add(key);
                propertyConnector.setValueByProp(key, map.get(key));
            }
        }
        if (subscriber != null) {
            subscriber.onPropertyUpdate(updatedProp);
        }
    }

}
