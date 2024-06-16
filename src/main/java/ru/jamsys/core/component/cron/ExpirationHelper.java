package ru.jamsys.core.component.cron;

import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

// Нам надо вызывать KeepAlive у ExpiredManager 1 раз в секунду, а не 1раз в 3сек

@Component
@Lazy
public class ExpirationHelper implements Cron1s, PromiseGenerator, ClassName {

    private final ManagerExpiration managerExpiration;

    private final ServicePromise servicePromise;

    @Setter
    private String index;

    public ExpirationHelper(
            ServicePromise servicePromise,
            ManagerExpiration managerExpiration
    ) {
        this.servicePromise = servicePromise;
        this.managerExpiration = managerExpiration;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 6_000L)
                .append("main", (isThreadRun, _) -> managerExpiration.keepAlive(isThreadRun));
    }

}
