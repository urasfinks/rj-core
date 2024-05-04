package ru.jamsys.core.component.promise.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.api.ClassFinder;
import ru.jamsys.core.extension.PingComponent;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskType;
import ru.jamsys.core.template.cron.release.Cron3s;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class Ping implements Cron3s, PromiseGenerator {

    private final List<PingComponent> list = new ArrayList<>();

    public Ping(ClassFinder classFinder, ApplicationContext applicationContext) {
        classFinder.findByInstance(PingComponent.class).forEach((Class<PingComponent> keepAliveClass)
                -> list.add(applicationContext.getBean(keepAliveClass)));
    }

    @Override
    public Promise generate() {
        return new PromiseImpl(getClass().getName())
                .append(this.getClass().getName(), PromiseTaskType.IO, (AtomicBoolean _)
                        -> list.forEach(ru.jamsys.core.extension.Ping::ping));
    }
}
