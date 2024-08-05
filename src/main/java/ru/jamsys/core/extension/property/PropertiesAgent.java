package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.item.PropertySubscriber;
import ru.jamsys.core.extension.property.repository.RepositoryMapValue;
import ru.jamsys.core.extension.property.repository.RepositoryProperties;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.*;
import java.util.function.Consumer;

// Агент связывает ServiceProperty и RepositoryProperties, задача агента донести изменённые свойства до полей подписчика
// Агент не обладает функционалом изменять свойства, для изменения используйте Property
// Агент - статичен по свойствам, что определено в PropertiesRepository - то и связано, ни больше не меньше

@Getter
public class PropertiesAgent implements LifeCycleInterface, PropertyUpdateDelegate {

    private final PropertyUpdateDelegate propertyUpdateDelegate;

    private final ServiceProperty serviceProperty;

    private final RepositoryProperties repositoryProperties;

    private final String ns;

    private final HashMap<String, PropertySubscriber> mapSubscription = new LinkedHashMap<>();

    private final HashMap<String, ExclusiveUpdate<?>> onExclusiveUpdate = new LinkedHashMap<>();

    public PropertiesAgent(
            ServiceProperty serviceProperty,
            PropertyUpdateDelegate propertyUpdateDelegate,
            RepositoryProperties repositoryProperties,
            String ns,
            boolean require
    ) {
        this.propertyUpdateDelegate = propertyUpdateDelegate;
        this.serviceProperty = serviceProperty;
        this.repositoryProperties = repositoryProperties;
        this.ns = ns;
        if (repositoryProperties != null) {
            repositoryProperties.setNs(ns);
        }
        init(require);
    }

    private void init(boolean require) {
        UtilRisc.forEach(null, this.repositoryProperties.getMapRepository(), (key, value) -> {
            @SuppressWarnings("unchecked")
            RepositoryMapValue<Object> x = (RepositoryMapValue<Object>) value;
            add(x.getCls(), key, x.getValue(), require, null);
        });
    }

    public List<PropertySubscriber> getListSubscriber() {
        List<PropertySubscriber> list = new ArrayList<>();
        for (String key : mapSubscription.keySet()) {
            if (serviceProperty.containsSubscriber(mapSubscription.get(key))) {
                list.add(mapSubscription.get(key));
            }
        }
        return list;
    }

    public <T> PropertiesAgent add(Class<T> cls, String repositoryKey, T defValue, boolean require, Consumer<T> onUpdate) {
        if (onUpdate != null) {
            onExclusiveUpdate.computeIfAbsent(repositoryKey, _ -> new ExclusiveUpdate<>(cls, onUpdate));
        }
        PropertySubscriber propertySubscriber = mapSubscription.computeIfAbsent(repositoryKey, k -> serviceProperty.subscribe(
                getServicePropertyKey(k),
                this,
                require,
                String.valueOf(defValue)
        ));
        serviceProperty.subscribe(propertySubscriber);
        return this;
    }

    public void series(String regexp){
        PropertySubscriber propertySubscriber = mapSubscription.computeIfAbsent(regexp, k -> serviceProperty.subscribe(
                k,
                this
        ));
        serviceProperty.subscribe(propertySubscriber);
    }

    public void removeByRepositoryKey(String repositoryKey) {
        PropertySubscriber remove = mapSubscription.remove(repositoryKey);
        serviceProperty.unsubscribe(remove);
    }

    public void removeByServicePropertiesKey(String propKey) {
        removeByRepositoryKey(getRepositoryKey(propKey));
    }

    public Set<String> getServicePropertyListeners() {
        Set<String> result = new LinkedHashSet<>();
        mapSubscription.forEach((s, _) -> result.add(getServicePropertyKey(s)));
        return result;
    }

    public Set<String> getRepositoryPropertyListeners() {
        Set<String> result = new LinkedHashSet<>();
        mapSubscription.forEach((s, _) -> result.add(s));
        return result;
    }

    public void onPropertyUpdate(Map<String, String> map) {
        Map<String, String> withoutNs = new HashMap<>();
        for (String key : map.keySet()) {
            // Бывает такое, что мы можем подписываться на чистый ns, так как не предполагается больше ключей
            String prop = getRepositoryKey(key);
            String value = map.get(key);
            repositoryProperties.setProperty(prop, value);
            ExclusiveUpdate<?> exclusiveUpdate = onExclusiveUpdate.get(prop);
            if (exclusiveUpdate != null) {
                exclusiveUpdate.onUpdate(value);
            }
            withoutNs.put(prop, value);
        }
        if (propertyUpdateDelegate != null && !withoutNs.isEmpty()) {
            propertyUpdateDelegate.onPropertyUpdate(withoutNs);
        }
    }

    // Получить ключик с ns
    private String getServicePropertyKey(String propKey) {
        if (propKey.isEmpty()) {
            if (ns == null) {
                // Не надо таких поворотов, когда и ns = null и ключ пустой
                // На что это ссылка получается в property? на на что?
                // Допустим есть ns = run.args.x1 и ключ пустота => подписываемся на run.args.x1
                // Если ns = "" и ключ = "" мы подписываемся на "" - а это исключено
                throw new RuntimeException("Определитесь либо ns = null либо key.isEmpty()");
            }
            return ns;
        } else {
            return ns != null ? (ns + "." + propKey) : propKey;
        }
    }

    // Получить ключик без ns
    private String getRepositoryKey(String propKey) {
        if (ns == null && propKey.isEmpty()) {
            throw new RuntimeException("Определитесь либо ns = null либо key.isEmpty()");
        } else if (ns == null) {
            return propKey;
        } else if (propKey.isEmpty()) {
            return ns;
        } else if (propKey.equals(ns)) {
            return "";
        } else {
            return propKey.substring(ns.length() + 1);
        }
    }

    public void setPropertyRepository(String key, String value) {
        serviceProperty.setProperty(getServicePropertyKey(key), value);
    }

    @Override
    public void run() {
        mapSubscription.forEach((_, propertySubscriber) -> serviceProperty.subscribe(propertySubscriber));
    }

    @Override
    public void shutdown() {
        mapSubscription.forEach((_, propertySubscriber) -> serviceProperty.unsubscribe(propertySubscriber));
    }

}
