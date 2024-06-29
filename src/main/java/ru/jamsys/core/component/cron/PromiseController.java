package ru.jamsys.core.component.cron;

import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Lazy
public class PromiseController implements Cron1s, PromiseGenerator, ClassName {

    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public PromiseController(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index,6_000L)
                .append("main", (AtomicBoolean isThreadRun, Promise _) -> {
                    AtomicInteger count = new AtomicInteger(0);
                    UtilRisc.forEach(isThreadRun, ServicePromise.queueMultipleCompleteSet, promise -> {
                        assert promise != null;
                        promise.complete();
                        count.incrementAndGet();
                    });
                });
    }

}
