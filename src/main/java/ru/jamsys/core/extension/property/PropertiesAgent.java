package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.repository.PropertiesRepository;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.*;
import java.util.function.Consumer;

// Агент связывает ServiceProperty и Subscriber, задача агента донести изменённые свойства до полей подписчика
// Агент не обладает функционалом изменять свойства, для изменения используйте Property

@Getter
public class PropertiesAgent implements LifeCycleInterface, PropertyUpdateDelegate {

    private final PropertyUpdateDelegate subscriber;

    private final ServiceProperty serviceProperty;

    private final PropertiesRepository propertiesRepository;

    private final String ns;

    private final HashMap<String, PropertyFollower> mapListener = new LinkedHashMap<>();

    private final HashMap<String, OnUpdate<?>> onUpdates = new LinkedHashMap<>();

    public int getCountListener() {
        int count = 0;
        for (String key : mapListener.keySet()) {
            if (serviceProperty.containsFollower(mapListener.get(key))) {
                count++;
            }
        }
        return count;
    }

    public PropertiesAgent(
            ServiceProperty serviceProperty,
            PropertyUpdateDelegate subscriber,
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

    private void init(boolean require) {
        UtilRisc.forEach(null, this.propertiesRepository.getPropValue(), (key, value) -> {
            add(String.class, key, value, require, null);
        });
    }

    public <T> PropertiesAgent add(Class<T> cls, String relativeKey, T defValue, boolean require, Consumer<T> onUpdate) {
        if (onUpdate != null) {
            onUpdates.computeIfAbsent(relativeKey, _ -> new OnUpdate<>(cls, onUpdate));
        }
        PropertyFollower propertyFollower = mapListener.computeIfAbsent(relativeKey, k -> serviceProperty.subscribe(
                getAbsoluteKey(k),
                this,
                require,
                String.valueOf(defValue)
        ));
        serviceProperty.subscribe(propertyFollower);
        return this;
    }

    public void removeRelative(String relativeKey) {
        PropertyFollower remove = mapListener.remove(relativeKey);
        serviceProperty.unsubscribe(remove);
    }

    public void removeAbsolute(String absoluteKey) {
        removeRelative(getRelativeKey(absoluteKey));
    }

    public Set<String> getKeySetAbsolute() {
        Set<String> result = new LinkedHashSet<>();
        mapListener.forEach((s, _) -> result.add(getAbsoluteKey(s)));
        return result;
    }

    public Set<String> getKeySetRelative() {
        Set<String> result = new LinkedHashSet<>();
        mapListener.forEach((s, _) -> result.add(s));
        return result;
    }

    public void onPropertyUpdate(Map<String, String> map) {
        Map<String, String> withoutNs = new HashMap<>();
        for (String key : map.keySet()) {
            // Бывает такое, что мы можем подписываться на чистый ns, так как не предполагается больше ключей
            String prop = getRelativeKey(key);
            String value = map.get(key);
            propertiesRepository.setPropValue(prop, value);
            OnUpdate<?> onUpdate = onUpdates.get(prop);
            if (onUpdate != null) {
                onUpdate.onUpdate(value);
            }
            withoutNs.put(prop, value);
        }
        if (subscriber != null && !withoutNs.isEmpty()) {
            subscriber.onPropertyUpdate(withoutNs);
        }
    }

    // Получить ключик с ns
    private String getAbsoluteKey(String relativeKey) {
        if (relativeKey.isEmpty()) {
            if (ns == null) {
                // Не надо таких поворотов, когда и ns = null и ключ пустой
                // На что это ссылка получается в property? на на что?
                // Допустим есть ns = run.args.x1 и ключ пустота => подписываемся на run.args.x1
                // Если ns = "" и ключ = "" мы подписываемся на "" - а это исключено
                throw new RuntimeException("Определитесь либо ns = null либо key.isEmpty()");
            }
            return ns;
        } else {
            return ns != null ? (ns + "." + relativeKey) : relativeKey;
        }
    }

    // Получить ключик без ns
    private String getRelativeKey(String absoluteKey) {
        if (ns == null && absoluteKey.isEmpty()) {
            throw new RuntimeException("Определитесь либо ns = null либо key.isEmpty()");
        } else if (ns == null) {
            return absoluteKey;
        } else if (absoluteKey.isEmpty()) {
            return ns;
        } else if (absoluteKey.equals(ns)) {
            return "";
        } else {
            return absoluteKey.substring(ns.length() + 1);
        }
    }

    public void setPropertyWithoutNs(String key, String value) {
        serviceProperty.setProperty(getAbsoluteKey(key), value);
    }

    @Override
    public void run() {
        mapListener.forEach((_, subscriberItem) -> serviceProperty.subscribe(subscriberItem));
    }

    @Override
    public void shutdown() {
        mapListener.forEach((_, subscriberItem) -> serviceProperty.unsubscribe(subscriberItem));
    }

}
