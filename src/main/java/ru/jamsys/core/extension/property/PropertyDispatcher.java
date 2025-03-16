package ru.jamsys.core.extension.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.item.PropertySubscription;
import ru.jamsys.core.extension.property.repository.PropertyEnvelopeRepository;
import ru.jamsys.core.extension.property.repository.PropertyRepository;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

// PropertySubscriber связывает ServiceProperty и PropertyRepository
// Задача донести изменения Property до PropertyRepository
// PropertySubscriber не обладает функционалом изменять свойства, для изменения используйте Property
// Получая элемент PropertyDispatcher вы должны начать контролировать его жизненный цикл самостоятельно run()/shutdown()
// Классная функция - это использовать namespace, не надо в репозитории использовать абсолютные ключи Property

@Getter
public class PropertyDispatcher<T> implements LifeCycleInterface {

    @JsonIgnore
    private final PropertyListener propertyListener;

    @JsonIgnore
    private final ServiceProperty serviceProperty;

    private final PropertyRepository<T> propertyRepository;

    private final String namespace;

    private final AtomicBoolean run = new AtomicBoolean(false);

    private final Set<PropertySubscription<T>> subscriptions = Util.getConcurrentHashSet();

    private final Set<PropertySubscription<T>> regexp = Util.getConcurrentHashSet();

    public PropertyDispatcher(
            ServiceProperty serviceProperty,
            PropertyListener propertyListener,
            PropertyRepository<T> propertyRepository,
            String namespace
    ) {
        this.propertyListener = propertyListener;
        this.serviceProperty = serviceProperty;
        this.propertyRepository = propertyRepository;
        this.namespace = namespace;

        if (this.propertyRepository != null) {
            this.propertyRepository.init(this);
            this.propertyRepository.checkNotNull();
        }
    }

    // Подписки не вызывают onPropertyUpdate.
    // Подписаться на серию Property по регулярному выражению
    public PropertyDispatcher<T> addSubscriptionRegexp(String regexp) {
        this.regexp.add(
                new PropertySubscription<>(this)
                        .setRegexp(regexp));
        UtilRisc.forEach(null, serviceProperty.getByRegexp(regexp), property -> {
            propertyRepository.append(getRepositoryPropertyKey(property.getKey()), this);
        });
        if (isRun()) {
            reload();
        }
        return this;
    }

    @SuppressWarnings("all")
    public PropertyDispatcher<T> removeSubscriptionByRepositoryPropertyKey(String repositoryPropertyKey) {
        return removeSubscriptionByPropertyKey(getPropertyKey(repositoryPropertyKey));
    }

    @SuppressWarnings("unused")
    public PropertyDispatcher<T> removeSubscriptionByPropertyKey(String propertyKey) {
        UtilRisc.forEach(null, subscriptions, propertySubscription -> {
            if (propertySubscription.getPropertyKey().equals(propertyKey)) {
                subscriptions.remove(propertySubscription);
                serviceProperty.removeSubscription(propertySubscription);
            }
        });
        return this;
    }

    // Вызывается из PropertySubscription, когда приходит уведомление от Property что значение изменено
    public void onPropertyUpdate(String propertyKey, String oldValue, String newValue) {
        String repositoryPropertyKey = getRepositoryPropertyKey(propertyKey);
        if (propertyRepository != null) {
            propertyRepository.updateRepository(repositoryPropertyKey, this);
        }
        if (propertyListener != null) {
            propertyListener.onPropertyUpdate(repositoryPropertyKey, oldValue, newValue);
        }
    }

    @SuppressWarnings("unused")
    public String getPropertyKey(PropertyEnvelopeRepository<T> propertyEnvelopeRepository) {
        if (propertyEnvelopeRepository == null) {
            throw new RuntimeException("PropertyEnvelopeRepository is null");
        }
        PropertyRepository<T> propertyRepository = getPropertyRepository();
        if (propertyRepository == null) {
            throw new RuntimeException("PropertyRepository is null");
        }
        return getPropertyKey(propertyEnvelopeRepository.getRepositoryPropertyKey());
    }

    // Получить ключик с ns, как будет полностью выглядеть ключ в .properties
    public String getPropertyKey(String repositoryPropertyKey) {
        if (repositoryPropertyKey.isEmpty()) {
            if (namespace == null) {
                // Не надо таких поворотов, когда и ns = null и ключ пустой
                // На что это ссылка получается в property?
                // Допустим есть ns = run.args.x1 и ключ пустота => подписываемся на run.args.x1
                // Если ns = "" и ключ = "" мы подписываемся на "" - а это исключено
                throw new RuntimeException("Определитесь либо ns = null либо key.isEmpty()");
            }
            return namespace;
        } else {
            return namespace != null ? (namespace + "." + repositoryPropertyKey) : repositoryPropertyKey;
        }
    }

    // Получить ключик без ns, как он числится в репозитории
    private String getRepositoryPropertyKey(String propertyKey) {
        if (namespace == null && propertyKey.isEmpty()) {
            throw new RuntimeException("Определитесь либо ns = null либо key.isEmpty()");
        } else if (namespace == null) {
            return propertyKey;
        } else if (propertyKey.isEmpty()) {
            return namespace;
        } else if (propertyKey.equals(namespace)) {
            return "";
        } else {
            return propertyKey.substring(namespace.length() + 1);
        }
    }

    @Override
    public boolean isRun() {
        return run.get();
    }

    @Override
    public void run() {
        if (run.compareAndSet(false, true)) {
            UtilRisc.forEach(null, regexp, serviceProperty::addSubscription);
            UtilRisc.forEach(null, this.propertyRepository.getListPropertyEnvelopeRepository(), propertyEnvelopeRepository -> {
                PropertySubscription<T> propertySubscription = new PropertySubscription<>(this)
                        .setPropertyKey(propertyEnvelopeRepository.getPropertyKey());
                serviceProperty.addSubscription(propertySubscription);
                subscriptions.add(propertySubscription);
            });

            UtilRisc.forEach(null, propertyRepository.getListPropertyEnvelopeRepository(), tPropertyEnvelopeRepository -> {
                ServiceProperty.Equals equals = tPropertyEnvelopeRepository.propertyEquals();
                if (!equals.isEquals()) {
                    onPropertyUpdate(
                            tPropertyEnvelopeRepository.getPropertyKey(),
                            equals.getOldValue(),
                            equals.getNewValue()
                    );
                }
            });
        }
    }

    @Override
    public void shutdown() {
        if (run.compareAndSet(true, false)) {
            UtilRisc.forEach(null, subscriptions, serviceProperty::removeSubscription);
            UtilRisc.forEach(null, regexp, serviceProperty::removeSubscription);
            subscriptions.clear();
        }
    }

}
