package ru.jamsys.core.component.promise.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.api.ExpiredManager;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskType;
import ru.jamsys.core.template.cron.release.Cron1s;

// Нам надо вызывать KeepAlive у ExpiredManager 1 раз в секунду, а не 1раз в 3сек

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class KeepAliveExpiredManager implements Cron1s, PromiseGenerator, ClassName {

    private final ExpiredManager<?> expiredManager;

    public KeepAliveExpiredManager(ApplicationContext applicationContext) {
        expiredManager = applicationContext.getBean(ExpiredManager.class);
    }

    @Override
    public Promise generate() {
        return new PromiseImpl(getClass().getName())
                .append(this.getClass().getName(), PromiseTaskType.IO, expiredManager::keepAlive);

    }

}
