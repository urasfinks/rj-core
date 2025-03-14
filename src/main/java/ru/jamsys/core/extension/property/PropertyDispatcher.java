package ru.jamsys.core.extension.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.item.PropertySubscription;
import ru.jamsys.core.extension.property.repository.PropertyRepository;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// PropertySubscriber связывает ServiceProperty и PropertyRepository
// Задача донести изменения Property до PropertyRepository
// PropertySubscriber не обладает функционалом изменять свойства, для изменения используйте Property
// Получая элемент PropertySubscriber вы должны начать контролировать его жизненный цикл самостоятельно run()/shutdown()
// Классная функция - это использовать namespace, не надо в репозитории использовать абсолютные ключи Property

@Getter
public class PropertyDispatcher implements LifeCycleInterface {

    @JsonIgnore
    private final PropertyListener propertyListener;

    @JsonIgnore
    private final ServiceProperty serviceProperty;

    private final PropertyRepository propertyRepository;

    private final String namespace;

    private final AtomicBoolean run = new AtomicBoolean(false);

    private final HashMap<String, PropertySubscription> subscriptions = new LinkedHashMap<>();

    public PropertyDispatcher(
            ServiceProperty serviceProperty,
            PropertyListener propertyListener,
            PropertyRepository propertyRepository,
            String namespace
    ) {
        this.propertyListener = propertyListener;
        this.serviceProperty = serviceProperty;
        this.propertyRepository = propertyRepository;
        this.namespace = namespace;

        if (this.propertyRepository != null) {
            UtilRisc.forEach(null, this.propertyRepository.getRepository(), this::addSubscription);
            try {
                this.propertyRepository.checkNotNull();
            } catch (Throwable th) {
                Map<String, String> result;
                if (namespace != null) {
                    result = new LinkedHashMap<>();
                    UtilRisc.forEach(null, this.propertyRepository.getRepository(), (key, value) -> {
                        result.put(getPropertyKey(key), value);
                    });
                } else {
                    result = this.propertyRepository.getRepository();
                }
                throw new ForwardException("PropertyRepository: " + UtilJson.toStringPretty(result, "{}"), th);
            }
        }
    }

    public String getPropertyKeyByRepositoryKey(String keyRepository) {
        if (getPropertyRepository() == null) {
            throw new RuntimeException("PropertyRepository is null");
        }
        if (!getPropertyRepository().getRepository().containsKey(keyRepository)) {
            throw new RuntimeException("Not found key: " + keyRepository + "; in repository ns: " + getPropertyRepository());
        }
        return getPropertyKey(keyRepository);
    }

    // Так как сам репопозиторий не знает в каком namespace он работает, нам необходимо сделать прокси
    // для преобразования ключа
    public void setRepositoryProxy(String key, String value) {
        if (propertyRepository != null) {
            propertyRepository.setRepository(getRepositoryKey(key), value);
        }
    }

    // Подписки не вызывают onPropertySubscriptionUpdate, да PropertyRepository заполнится, но не более
    public PropertyDispatcher addSubscription(String key, String defaultValue) {
        PropertySubscription propertySubscription = new PropertySubscription(this, serviceProperty)
                .setPropertyKey(getPropertyKey(key))
                .setDefaultValue(defaultValue)
                .setDescription(propertyRepository != null ? propertyRepository.getDescription(key) : null)
                .syncPropertyRepository();
        subscriptions.put(key, propertySubscription);
        if (isRun()) {
            run();
        }
        return this;
    }

    // Подписки не вызывают onPropertySubscriptionUpdate, да PropertyRepository заполнится, но не более
    // Подписаться на серию настроек по регулярному выражению
    public PropertyDispatcher addSubscriptionRegexp(String regexp) {
        subscriptions.put(
                regexp,
                new PropertySubscription(this, serviceProperty)
                        .setRegexp(regexp)
                        .syncPropertyRepository()
        );
        if (isRun()) {
            run();
        }
        return this;
    }

    @SuppressWarnings("all")
    public PropertyDispatcher removeSubscriptionByRepositoryKey(String key) {
        PropertySubscription remove = subscriptions.remove(key);
        serviceProperty.removeSubscription(remove);
        return this;
    }

    @SuppressWarnings("unused")
    public PropertyDispatcher removeSubscriptionByPropertiesKey(String propKey) {
        removeSubscriptionByRepositoryKey(getRepositoryKey(propKey));
        return this;
    }

    // Вызывается из PropertySubscription, когда приходит уведомление от Property что значение изменено
    public void onPropertySubscriptionUpdate(String oldValue, Property property) {
        String repositoryKey = getRepositoryKey(property.getKey());
        if (propertyRepository != null) {
            propertyRepository.setRepository(repositoryKey, property.get());
        }
        if (propertyListener != null) {
            propertyListener.onPropertyUpdate(repositoryKey, oldValue, property);
        }
    }

    // Получить ключик с ns, как будет полностью выглядеть ключ в .properties
    private String getPropertyKey(String key) {
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

    // Получить ключик без ns, как он числится в репозитории
    private String getRepositoryKey(String propKey) {
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
    public boolean isRun() {
        return run.get();
    }

    @Override
    public void run() {
        run.set(true);
        subscriptions.forEach((_, subscription) -> serviceProperty.addSubscription(subscription));
    }

    @Override
    public void shutdown() {
        run.set(false);
        subscriptions.forEach((_, subscription) -> serviceProperty.removeSubscription(subscription));
    }

}
