package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.item.SubscriberItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Посредник между объектом, кто хочет получать уведомления от изменения свойств до ServiceProperty
// При получении событий по изменению свойств вызывает onPropertyUpdate у хозяина
// Решает проблему ключей с ns или без ns + хранит подписки, что бы в случаи удалении объекта произвести отписку
// Просто как дворецкий, ни больше не меньше

@Getter
public class PropertiesNsAgent implements LifeCycleInterface {

    private final PropertyUpdateDelegate subscriber;

    private final ServiceProperty serviceProperty;

    private final PropertiesRepository propertiesRepository;

    private final String ns;

    private final HashMap<String, SubscriberItem> mapListener = new HashMap<>();

    public int getCountListener() {
        int count = 0;
        for (String key : mapListener.keySet()) {
            if (mapListener.get(key).isSubscribe()) {
                count++;
            }
        }
        return count;
    }

    public PropertiesNsAgent(
            PropertyUpdateDelegate subscriber,
            ServiceProperty serviceProperty,
            PropertiesRepository propertiesRepository,
            String ns
    ) {
        this.subscriber = subscriber;
        this.serviceProperty = serviceProperty;
        this.propertiesRepository = propertiesRepository;
        this.ns = ns;
        init(true);
    }

    public PropertiesNsAgent(
            PropertyUpdateDelegate subscriber,
            ServiceProperty serviceProperty,
            PropertiesRepository propertiesRepository,
            String ns,
            boolean require
    ) {
        this.subscriber = subscriber;
        this.serviceProperty = serviceProperty;
        this.propertiesRepository = propertiesRepository;
        this.ns = ns;
        init(require);
    }

    public void setProperty(String key, String value) {
        this.serviceProperty.setProperty(getKeyWithNs(key), value);
    }

    private void init(boolean require) {
        Map<String, String> mapPropValue = this.propertiesRepository.getMapPropValue();
        for (String key : mapPropValue.keySet()) {
            add(key, mapPropValue.get(key), require);
        }
    }

    private String getKeyWithNs(String key) {
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

    public PropertiesNsAgent add(String key, String defValue, boolean require) {
        SubscriberItem subscriberItem = mapListener.computeIfAbsent(key, _ -> new SubscriberItem(defValue, require));
        if (!subscriberItem.isSubscribe()) {
            serviceProperty.subscribe(
                    getKeyWithNs(key),
                    this,
                    subscriberItem.isRequire(),
                    subscriberItem.getDefValue()
            );
            subscriberItem.setSubscribe(true);
        }
        return this;
    }

    public void remove(String key) {
        mapListener.remove(key);
        serviceProperty.unsubscribe(getKeyWithNs(key), this);
    }

    public void onPropertyUpdate(Map<String, String> map) {
        Set<String> updatedProp = new HashSet<>();
        if (ns != null) {
            for (String key : map.keySet()) {
                // Бывает такое, что мы можем подписываться на чистый ns, так как не предполагается больше ключей
                String prop = key.equals(ns) ? "" : key.substring(ns.length() + 1);
                updatedProp.add(prop);
                propertiesRepository.setValueByProp(prop, map.get(key));
            }

        } else {
            for (String key : map.keySet()) {
                updatedProp.add(key);
                propertiesRepository.setValueByProp(key, map.get(key));
            }
        }
        if (subscriber != null && !updatedProp.isEmpty()) {
            subscriber.onPropertyUpdate(updatedProp);
        }
    }

    @Override
    public void run() {
        mapListener.forEach((key, subscriberItem) -> {
            if (!subscriberItem.isSubscribe()) {
                serviceProperty.subscribe(
                        getKeyWithNs(key),
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
        mapListener.forEach((key, subscriberItem) -> {
            if (subscriberItem.isSubscribe()) {
                serviceProperty.unsubscribe(getKeyWithNs(key), this);
                subscriberItem.setSubscribe(false);
            }
        });
    }

}