package ru.jamsys.core.component.cron;

import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.flat.template.cron.release.Cron3s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class KeepAlive implements Cron3s, PromiseGenerator, UniqueClassName {

    private final List<KeepAliveComponent> list = new ArrayList<>();

    private final ServicePromise servicePromise;

    @Setter
    private String index;

    public KeepAlive(ServiceClassFinder serviceClassFinder, ApplicationContext applicationContext, ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
        serviceClassFinder.findByInstance(KeepAliveComponent.class).forEach((Class<KeepAliveComponent> keepAliveClass)
                -> list.add(applicationContext.getBean(keepAliveClass)));
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index,6_000L)
                .append("main", (AtomicBoolean isThreadRun, Promise _)
                        -> list.forEach((KeepAliveComponent keepAliveComponent)
                        -> keepAliveComponent.keepAlive(isThreadRun)));
    }

}
