package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Посредник между объектом, кто хочет получать уведомления от изменения свойств до ServiceProperty
// При получении событий по изменению свойств вызывает onPropertyUpdate у хозяина
// Решает проблему ключей с ns или без ns + хранит подписки, что бы в случаи удалении объекта произвести отписку
// Просто как дворецкий, ни больше не меньше

@Getter
public class Subscriber implements LifeCycleInterface {

    private final PropertySubscriberNotify subscriber;

    private final ServiceProperty serviceProperty;

    private final PropertyConnector propertyConnector;

    private final String ns;

    private final HashMap<String, SubscriberItem> subscriptions = new HashMap<>();

    public int getCountSubscribe() {
        int count = 0;
        for (String key : subscriptions.keySet()) {
            if (subscriptions.get(key).isSubscribe()) {
                count++;
            }
        }
        return count;
    }

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

    private void init(boolean require) {
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

    public Subscriber subscribe(String key, String defValue, boolean require) {
        SubscriberItem subscriberItem = subscriptions.computeIfAbsent(key, _ -> new SubscriberItem(defValue, require));
        if (!subscriberItem.isSubscribe()) {
            serviceProperty.subscribe(
                    getKeyWithNamespace(key),
                    this,
                    subscriberItem.isRequire(),
                    subscriberItem.getDefValue()
            );
            subscriberItem.setSubscribe(true);
        }
        return this;
    }

    public void unsubscribe(String key) {
        subscriptions.remove(key);
        serviceProperty.unsubscribe(getKeyWithNamespace(key), this);
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
        if (subscriber != null && !updatedProp.isEmpty()) {
            subscriber.onPropertyUpdate(updatedProp);
        }
    }

    @Override
    public void run() {
        subscriptions.forEach((key, subscriberItem) -> {
            if (!subscriberItem.isSubscribe()) {
                serviceProperty.subscribe(
                        getKeyWithNamespace(key),
                        this,
                        subscriberItem.isRequire(),
                        subscriberItem.getDefValue()
                );
                subscriberItem.setSubscribe(true);
            }
        });
    }

    @Override
    public void shutdown() {
        subscriptions.forEach((key, subscriberItem) -> {
            if (subscriberItem.isSubscribe()) {
                serviceProperty.unsubscribe(getKeyWithNamespace(key), this);
                subscriberItem.setSubscribe(false);
            }
        });
    }

}
