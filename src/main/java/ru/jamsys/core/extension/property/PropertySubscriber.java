package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.item.PropertySubscription;
import ru.jamsys.core.extension.property.repository.PropertyRepository;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

// PropertySubscriber связывает ServiceProperty и PropertyRepository
// Задача донести изменения Property до PropertyRepository
// PropertySubscriber не обладает функционалом изменять свойства, для изменения используйте Property
// Получая элемент PropertySubscriber вы должны начать контролировать его жизненный цикл самостоятельно run()/shutdown()

@Getter
public class PropertySubscriber implements LifeCycleInterface {

    private final PropertyUpdater propertyUpdater;

    private final ServiceProperty serviceProperty;

    private final PropertyRepository propertyRepository;

    private final String namespace;

    private final HashMap<String, PropertySubscription> subscriptions = new LinkedHashMap<>();

    public PropertySubscriber(
            ServiceProperty serviceProperty,
            PropertyUpdater propertyUpdater,
            PropertyRepository propertyRepository,
            String namespace
    ) {
        this.propertyUpdater = propertyUpdater;
        this.serviceProperty = serviceProperty;
        this.propertyRepository = propertyRepository;
        this.namespace = namespace;

        if (this.propertyRepository != null) {
            UtilRisc.forEach(null, this.propertyRepository.getRepository(), (key, defaultValue) -> {
                String propertyKey = getPropertyKey(key);
                // Получили default значение, получили Property, если не сошлись, считаем приоритетным Property.get()
                String propertyValue = serviceProperty.get(
                        propertyKey,
                        defaultValue,
                        "stack: " + getClass().getName()
                                + "; repository:" + propertyRepository.getClass().getName()
                                + "; namespace: " + namespace
                ).get();
                if (!Objects.equals(defaultValue, propertyValue)) {
                    this.propertyRepository.setRepository(key, propertyValue);
                }
                addSubscription(key);
            });
            this.propertyRepository.checkNotNull();
        }
    }

    public PropertySubscriber addSubscription(String key) {
        // За регистрацию в ServiceProperty отвечает run()
        subscriptions.put(
                key,
                new PropertySubscription(this).setKey(getPropertyKey(key))
        );
        return this;
    }

    // Подписаться на серию настроек по регулярному выражению
    public PropertySubscriber addSubscriptionPattern(String regexp) {
        // За регистрацию в ServiceProperty отвечает run()
        subscriptions.put(
                regexp,
                new PropertySubscription(this).setKeyPattern(regexp)
        );
        return this;
    }

    public PropertySubscriber removeByRepositoryKey(String key) {
        PropertySubscription remove = subscriptions.remove(key);
        serviceProperty.removeSubscription(remove);
        return this;
    }

    public PropertySubscriber removeByPropertiesKey(String propKey) {
        removeByRepositoryKey(getRepositoryKey(propKey));
        return this;
    }

    public void onPropertySubscriptionUpdate(Property property) {
        String repositoryKey = getRepositoryKey(property.getKey());
        if (propertyRepository != null) {
            propertyRepository.setRepository(repositoryKey, property.get());
        }
        if (propertyUpdater != null) {
            propertyUpdater.onPropertyUpdate(repositoryKey, property);
        }
    }

    // Получить ключик с ns, как будет полностью выглядеть ключ в .properties
    public String getPropertyKey(String key) {
        if (key.isEmpty()) {
            if (namespace == null) {
                // Не надо таких поворотов, когда и ns = null и ключ пустой
                // На что это ссылка получается в property? на на что?
                // Допустим есть ns = run.args.x1 и ключ пустота => подписываемся на run.args.x1
                // Если ns = "" и ключ = "" мы подписываемся на "" - а это исключено
                throw new RuntimeException("Определитесь либо ns = null либо key.isEmpty()");
            }
            return namespace;
        } else {
            return namespace != null ? (namespace + "." + key) : key;
        }
    }

    // Получить ключик без ns, как он числится в объекте
    public String getRepositoryKey(String propKey) {
        if (namespace == null && propKey.isEmpty()) {
            throw new RuntimeException("Определитесь либо ns = null либо key.isEmpty()");
        } else if (namespace == null) {
            return propKey;
        } else if (propKey.isEmpty()) {
            return namespace;
        } else if (propKey.equals(namespace)) {
            return "";
        } else {
            return propKey.substring(namespace.length() + 1);
        }
    }

    @Override
    public void run() {
        subscriptions.forEach((_, subscription) -> serviceProperty.addSubscription(subscription));
    }

    @Override
    public void shutdown() {
        subscriptions.forEach((_, subscription) -> serviceProperty.removeSubscription(subscription));
    }

}
