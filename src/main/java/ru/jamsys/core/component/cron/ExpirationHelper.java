package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

// Нам надо вызывать KeepAlive у ExpiredManager 1 раз в секунду, а не 1раз в 3сек

@SuppressWarnings("unused")
@Component
@Lazy
public class ExpirationHelper implements Cron1s, PromiseGenerator, UniqueClassName {

    private final ManagerExpiration managerExpiration;

    private final ServicePromise servicePromise;

    public ExpirationHelper(
            ServicePromise servicePromise,
            ManagerExpiration managerExpiration
    ) {
        this.servicePromise = servicePromise;
        this.managerExpiration = managerExpiration;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 6_000L)
                .append("main", (threadRun, _, _) -> managerExpiration.keepAlive(threadRun));
    }

}
