package ru.jamsys.core.component.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.ExpirationManager;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskExecuteType;

// Нам надо вызывать KeepAlive у ExpiredManager 1 раз в секунду, а не 1раз в 3сек

@Component
public class KeepAliveExpirationManager implements Cron1s, PromiseGenerator, ClassName {

    private final ExpirationManager expirationManager;

    public KeepAliveExpirationManager(ApplicationContext applicationContext) {
        expirationManager = applicationContext.getBean(ExpirationManager.class);
    }

    @Override
    public Promise generate() {
        return new PromiseImpl(getClass().getName(), 6_000L)
                .append(this.getClass().getName(), PromiseTaskExecuteType.IO, (isThreadRun, promise) -> expirationManager.keepAlive(isThreadRun));
    }

}
