package ru.jamsys.core.component.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ClassFinderComponent;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskType;
import ru.jamsys.core.flat.template.cron.release.Cron3s;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class KeepAlive implements Cron3s, PromiseGenerator {

    private final List<KeepAliveComponent> list = new ArrayList<>();

    public KeepAlive(ClassFinderComponent classFinderComponent, ApplicationContext applicationContext) {
        classFinderComponent.findByInstance(KeepAliveComponent.class).forEach((Class<KeepAliveComponent> keepAliveClass)
                -> list.add(applicationContext.getBean(keepAliveClass)));
    }

    @Override
    public Promise generate() {
        return new PromiseImpl(getClass().getName(),6_000L)
                .append(this.getClass().getName(), PromiseTaskType.IO, (AtomicBoolean isThreadRun)
                        -> list.forEach((KeepAliveComponent keepAliveComponent)
                        -> keepAliveComponent.keepAlive(isThreadRun)));
    }

}
