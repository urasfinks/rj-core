package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Посредник между объектом, кто хочет получать уведомления от изменения свойств до ServiceProperty
// При получении событий по изменению свойств вызывает onPropertyUpdate у хозяина
// Решает проблему ключей с ns или без ns + хранит подписки, что бы в случаи удалении объекта произвести отписку
// Просто как дворецкий, ни больше не меньше

@Getter
public class Subscriber {

    private final PropertySubscriberNotify subscriber;

    private final ServiceProperty serviceProperty;

    private final Set<String> subscriptions = new HashSet<>();

    private final PropertyConnector propertyConnector;

    private final String ns;

    public Subscriber(
            PropertySubscriberNotify subscriber,
            ServiceProperty serviceProperty,
            PropertyConnector propertyConnector,
            String ns
    ) {
        this.subscriber = subscriber;
        this.serviceProperty = serviceProperty;
        this.propertyConnector = propertyConnector;
        this.ns = ns;
        init(true);
    }

    public Subscriber(
            PropertySubscriberNotify subscriber,
            ServiceProperty serviceProperty,
            PropertyConnector propertyConnector,
            String ns,
            boolean require
    ) {
        this.subscriber = subscriber;
        this.serviceProperty = serviceProperty;
        this.propertyConnector = propertyConnector;
        this.ns = ns;
        init(require);
    }

    public void setProperty(String key, String value) {
        this.serviceProperty.setProperty(getKeyWithNamespace(key), value);
    }

    public void init(boolean require) {
        Map<String, String> mapPropValue = this.propertyConnector.getMapPropValue();
        for (String key : mapPropValue.keySet()) {
            subscribe(key, mapPropValue.get(key), require);
        }
    }

    private String getKeyWithNamespace(String key) {
        if (key.isEmpty()) {
            if (ns == null) {
                // Не надо таких поворотов, когда и ns = null и ключ пустой
                // На что это ссылка получается в property? на на что?
                // Допустим есть ns = run.args.x1 и ключ пустота => подписываемся на run.args.x1
                // Если ns = "" и ключ = "" мы подписываемся на "" - а это исключено
                throw new RuntimeException("Определитесь либо ns = null либо key.isEmpty()");
            }
            return ns;
        } else {
            return ns != null ? (ns + "." + key) : key;
        }
    }

    public Subscriber subscribe(String key, boolean require) {
        return subscribe(key, null, require);
    }

    public Subscriber subscribe(String key, String defValue, boolean require) {
        if (!subscriptions.contains(key)) {
            subscriptions.add(key);
            serviceProperty.subscribe(getKeyWithNamespace(key), this, require, defValue);
        }
        return this;
    }

    public void unsubscribe(String key) {
        subscriptions.remove(key);
        serviceProperty.unsubscribe(getKeyWithNamespace(key), this);
    }

    public void unsubscribe() {
        for (String key : subscriptions) {
            serviceProperty.unsubscribe(getKeyWithNamespace(key), this);
        }
        subscriptions.clear();
    }

    public void onServicePropertyUpdate(Map<String, String> map) {
        Set<String> updatedProp = new HashSet<>();
        if (ns != null) {
            for (String key : map.keySet()) {
                // Бывает такое, что мы можем подписываться на чистый ns, так как не предполагается больше ключей
                String prop = key.equals(ns) ? "" : key.substring(ns.length() + 1);
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
