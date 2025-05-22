package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.expiration.ExpirationList;
import ru.jamsys.core.promise.AbstractPromiseTask;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

// Сервис управления перезапуска задач

@Component
@Lazy
public class ServicePromise implements CascadeKey {

    @SuppressWarnings("all")
    private final Manager.Configuration<ExpirationList<Promise>> timeOutExpirationList;

    @SuppressWarnings("all")
    private final Manager.Configuration<ExpirationList<AbstractPromiseTask>> retryExporationList; // Задачи на повтор

    public ServicePromise(ApplicationContext applicationContext) {
        timeOutExpirationList = ExpirationList.getInstanceConfigure(
                applicationContext,
                getCascadeKey("timeOut"),
                Promise::timeOut
        );
        retryExporationList = ExpirationList.getInstanceConfigure(
                applicationContext,
                getCascadeKey("retry"),
                promiseTask -> promiseTask.prepareLaunch(null)
        );
    }

    public Promise get(Class<?> cls, long timeOutMs) {
        return get(App.getUniqueClassName(cls), timeOutMs);
    }

    public Promise get(String index, long timeOutMs) {
        Promise promise = new Promise(index, timeOutMs);
        promise.setRegisteredTimeOutExpiration(timeOutExpirationList.get().add(promise, timeOutMs));
        return promise;
    }

    public boolean removeTimeout(DisposableExpirationMsImmutableEnvelope<Promise> envelopeTimeout) {
        return timeOutExpirationList.get().remove(envelopeTimeout);
    }

    public void addRetryDelay(AbstractPromiseTask promiseTask) {
        retryExporationList.get().add(new ExpirationMsImmutableEnvelope<>(promiseTask, promiseTask.getRetryDelayMs()));
    }

}
