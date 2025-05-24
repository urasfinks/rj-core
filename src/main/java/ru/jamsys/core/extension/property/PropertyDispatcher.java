package ru.jamsys.core.extension.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.repository.AbstractRepositoryProperty;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.Set;

// PropertySubscriber связывает ServiceProperty и PropertyRepository
// Задача донести изменения Property до PropertyRepository
// PropertySubscriber не обладает функционалом изменять свойства, для изменения используйте Property
// Получая элемент PropertyDispatcher вы должны начать контролировать его жизненный цикл самостоятельно run()/shutdown()
// Классная функция - это использовать namespace, не надо в репозитории использовать абсолютные ключи Property

@Getter
public class PropertyDispatcher<T> extends AbstractLifeCycle implements LifeCycleInterface {

    @JsonIgnore
    private final PropertyListener propertyListener;

    @JsonIgnore
    private final ServiceProperty serviceProperty;

    private final AbstractRepositoryProperty<T> repositoryProperty;

    private final String ns;

    private final Set<PropertySubscription<T>> subscriptions = Util.getConcurrentHashSet();

    private final Set<PropertySubscription<T>> regexp = Util.getConcurrentHashSet();

    public PropertyDispatcher(
            PropertyListener propertyListener,
            AbstractRepositoryProperty<T> repositoryProperty,
            String ns
    ) {
        this.propertyListener = propertyListener;
        this.serviceProperty = App.get(ServiceProperty.class);
        this.repositoryProperty = repositoryProperty;
        this.ns = ns;

        if (this.repositoryProperty != null) {
            this.repositoryProperty.init(this);
            this.repositoryProperty.checkNotNull();
        }
    }

    // Подписки не вызывают onPropertyUpdate.
    // Подписаться на серию Property по регулярному выражению
    public PropertyDispatcher<T> addSubscriptionRegexp(String regexp) {
        this.regexp.add(
                new PropertySubscription<>(this)
                        .setRegexp(regexp));
        UtilRisc.forEach(null, serviceProperty.getByRegexp(regexp), property -> {
            repositoryProperty.append(getRepositoryPropertyKey(property.getKey()), this);
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
        if (repositoryProperty != null) {
            repositoryProperty.updateRepository(repositoryPropertyKey, this);
        }
        if (propertyListener != null) {
            propertyListener.onPropertyUpdate(repositoryPropertyKey, oldValue, newValue);
        }
    }

    @SuppressWarnings("unused")
    public String getPropertyKey(PropertyEnvelope<T> propertyEnvelope) {
        if (propertyEnvelope == null) {
            throw new RuntimeException("PropertyEnvelopeRepository is null");
        }
        AbstractRepositoryProperty<T> repositoryProperty = getRepositoryProperty();
        if (repositoryProperty == null) {
            throw new RuntimeException("PropertyRepository is null");
        }
        return getPropertyKey(propertyEnvelope.getRepositoryPropertyKey());
    }

    // Получить ключик с ns, как будет полностью выглядеть ключ в .properties
    public String getPropertyKey(String repositoryPropertyKey) {
        if (repositoryPropertyKey.isEmpty()) {
            if (ns == null) {
                // Не надо таких поворотов, когда и ns = null и ключ пустой
                // На что это ссылка получается в property?
                // Допустим есть ns = run.args.x1 и ключ пустота => подписываемся на run.args.x1
                // Если ns = "" и ключ = "" мы подписываемся на "" - а это исключено
                throw new RuntimeException("Определитесь либо ns = null либо key.isEmpty()");
            }
            return ns;
        } else {
            return ns != null ? (ns + "." + repositoryPropertyKey) : repositoryPropertyKey;
        }
    }

    // Получить ключик без ns, как он числится в репозитории
    private String getRepositoryPropertyKey(String propertyKey) {
        if (ns == null && propertyKey.isEmpty()) {
            throw new RuntimeException("Определитесь либо ns = null либо key.isEmpty()");
        } else if (ns == null) {
            return propertyKey;
        } else if (propertyKey.isEmpty()) {
            return ns;
        } else if (propertyKey.equals(ns)) {
            return "";
        } else {
            return propertyKey.substring(ns.length() + 1);
        }
    }

    @Override
    public void runOperation() {
        UtilRisc.forEach(null, regexp, serviceProperty::addSubscription);
        UtilRisc.forEach(null, this.repositoryProperty.getListPropertyEnvelopeRepository(), propertyEnvelopeRepository -> {
            PropertySubscription<T> propertySubscription = new PropertySubscription<>(this)
                    .setPropertyKey(propertyEnvelopeRepository.getPropertyKey());
            serviceProperty.addSubscription(propertySubscription);
            subscriptions.add(propertySubscription);
        });

        UtilRisc.forEach(null, repositoryProperty.getListPropertyEnvelopeRepository(), tPropertyEnvelopeRepository -> {
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

    @Override
    public void shutdownOperation() {
        UtilRisc.forEach(null, subscriptions, serviceProperty::removeSubscription);
        UtilRisc.forEach(null, regexp, serviceProperty::removeSubscription);
        subscriptions.clear();
    }

}
