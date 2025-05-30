package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.expiration.ExpirationList;
import ru.jamsys.core.promise.AbstractPromiseTask;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.extension.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.extension.expiration.immutable.ExpirationMsImmutableEnvelope;

// Сервис управления перезапуска задач

@Component
@Lazy
public class ServicePromise implements CascadeKey {

    @SuppressWarnings("all")
    private final ManagerConfiguration<ExpirationList<Promise>> timeOutExpirationList;

    @SuppressWarnings("all")
    private final ManagerConfiguration<ExpirationList<AbstractPromiseTask>> retryExporationList; // Задачи на повтор

    public ServicePromise() {
        // Так как это компонент, у нас не может быть множества экземпляров key будет константой
        timeOutExpirationList = ManagerConfiguration.getInstance(
                ExpirationList.class,
                ServicePromise.class.getName(),
                getCascadeKey("timeOut"),
                promiseExpirationList -> promiseExpirationList.setupOnExpired(Promise::timeOut)
        );
        retryExporationList = ManagerConfiguration.getInstance(
                ExpirationList.class,
                ServicePromise.class.getName(),
                getCascadeKey("retry"),
                abstractPromiseTaskExpirationList -> abstractPromiseTaskExpirationList
                        .setupOnExpired(promiseTask -> promiseTask.prepareLaunch(null))
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
