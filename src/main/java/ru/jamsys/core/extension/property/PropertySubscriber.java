package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.item.PropertySubscription;
import ru.jamsys.core.extension.property.repository.PropertyRepository;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.HashMap;
import java.util.LinkedHashMap;

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
            UtilRisc.forEach(null, this.propertyRepository.getRepository(), this::addSubscription);
            this.propertyRepository.checkNotNull();
        }
    }

    private String getWho() {
        StringBuilder sb = new StringBuilder();
        sb
                .append("class: ").append(getClass().getName()).append("; ")
                .append("namespace: ").append(namespace).append("; ");
        if (propertyRepository != null) {
            sb
                    .append("repository: ").append(propertyRepository.getClass().getName()).append("; ");
        }
        return sb.toString();
    }

    // Так как сам репопозиторий не знает в каком namespace он работает, нам необходимо сделать прокси
    // для преобразования ключа
    public void setRepositoryProxy(String key, String value) {
        if (propertyRepository != null) {
            propertyRepository.setRepository(getRepositoryKey(key), value);
        }
    }

    public PropertySubscriber addSubscription(String key, String defaultValue) {
        // За регистрацию в ServiceProperty отвечает run()
        PropertySubscription propertySubscription = new PropertySubscription(this, serviceProperty)
                .setPropertyKey(getPropertyKey(key))
                .setDefaultValue(defaultValue)
                .syncPropertyRepository(getWho());
        subscriptions.put(key, propertySubscription);
        return this;
    }

    // Подписаться на серию настроек по регулярному выражению
    public PropertySubscriber addSubscriptionRegexp(String regexp) {
        // За регистрацию в ServiceProperty отвечает run()
        subscriptions.put(
                regexp,
                new PropertySubscription(this, serviceProperty)
                        .setRegexp(regexp)
                        .syncPropertyRepository(getWho())
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
