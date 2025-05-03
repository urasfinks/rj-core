package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.expiration.ExpirationList;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.promise.AbstractPromiseTask;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

// Сервис управления перезапуска задач

@Component
@Lazy
public class ServicePromise implements CascadeKey {

    @SuppressWarnings("all")
    private final Manager.Configuration<ExpirationList> timeOutExpirationList;

    @SuppressWarnings("all")
    private final Manager.Configuration<ExpirationList> retryExporationList;

    public ServicePromise(Manager manager) {
        timeOutExpirationList = manager.configure(
                ExpirationList.class,
                getCascadeKey("timeOut"),
                (ns1) -> new ExpirationList<>(ns1, this::onTimeOut)
        );
        retryExporationList = manager.configure(
                ExpirationList.class,
                getCascadeKey("retry"),
                (key1) -> new ExpirationList<>(key1, this::onRetry)
        );
    }

    public Promise get(Class<?> cls, long timeOutMs) {
        return get(App.getUniqueClassName(cls), timeOutMs);
    }

    @SuppressWarnings("unchecked")
    public Promise get(String index, long timeOutMs) {
        Promise promise = new Promise(index, timeOutMs);
        promise.setRegisteredTimeOutExpiration(timeOutExpirationList.get().add(promise, timeOutMs));
        return promise;
    }

    @SuppressWarnings("unchecked")
    public boolean removeTimeout(DisposableExpirationMsImmutableEnvelope<Promise> envelopeTimeout) {
        return timeOutExpirationList.get().remove(envelopeTimeout);
    }

    @SuppressWarnings("unchecked")
    public void addRetryDelay(AbstractPromiseTask promiseTask) {
        retryExporationList.get().add(new ExpirationMsImmutableEnvelope<>(promiseTask, promiseTask.getRetryDelayMs()));
    }

    private void onRetry(DisposableExpirationMsImmutableEnvelope<AbstractPromiseTask> env) {
        AbstractPromiseTask promiseTask = env.getValue();
        if (promiseTask != null) {
            promiseTask.prepareLaunch(null);
        }
    }

    private void onTimeOut(DisposableExpirationMsImmutableEnvelope<Promise> env) {
        Promise promise = env.getValue();
        if (promise != null) {
            promise.timeOut();
        }
    }

}
