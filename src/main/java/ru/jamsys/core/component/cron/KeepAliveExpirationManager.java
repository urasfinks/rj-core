package ru.jamsys.core.component.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.PromiseComponent;
import ru.jamsys.core.component.manager.ExpirationManager;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

// Нам надо вызывать KeepAlive у ExpiredManager 1 раз в секунду, а не 1раз в 3сек

@Component
@Lazy
public class KeepAliveExpirationManager implements Cron1s, PromiseGenerator, ClassName {

    private final ExpirationManager expirationManager;

    private final PromiseComponent promiseComponent;

    private final String index;

    public KeepAliveExpirationManager(ApplicationContext applicationContext, PromiseComponent promiseComponent) {
        this.promiseComponent = promiseComponent;
        expirationManager = applicationContext.getBean(ExpirationManager.class);
        index = getClassName("cron", applicationContext);
    }

    @Override
    public Promise generate() {
        return promiseComponent.get(index, 6_000L)
                .append(this.getClass().getName(), (isThreadRun, _) -> expirationManager.keepAlive(isThreadRun));
    }

}
