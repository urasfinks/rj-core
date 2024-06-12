package ru.jamsys.core.component.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ClassFinderComponent;
import ru.jamsys.core.component.PromiseComponent;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.flat.template.cron.release.Cron3s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class KeepAlive implements Cron3s, PromiseGenerator, ClassName {

    private final List<KeepAliveComponent> list = new ArrayList<>();

    private final String index;

    private final PromiseComponent promiseComponent;

    public KeepAlive(ClassFinderComponent classFinderComponent, ApplicationContext applicationContext, PromiseComponent promiseComponent) {
        this.promiseComponent = promiseComponent;
        index = getClassName("cron", applicationContext);
        classFinderComponent.findByInstance(KeepAliveComponent.class).forEach((Class<KeepAliveComponent> keepAliveClass)
                -> list.add(applicationContext.getBean(keepAliveClass)));
    }

    @Override
    public Promise generate() {
        return promiseComponent.get(index,6_000L)
                .append("KeepAlive", (AtomicBoolean isThreadRun, Promise _)
                        -> list.forEach((KeepAliveComponent keepAliveComponent)
                        -> keepAliveComponent.keepAlive(isThreadRun)));
    }

}
