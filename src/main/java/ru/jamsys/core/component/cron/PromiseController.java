package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Component
@Lazy
public class PromiseController implements Cron1s, PromiseGenerator, UniqueClassName {

    private final ServicePromise servicePromise;

    public PromiseController(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 6_000L)
                .append("main", (threadRun, _, _) -> {
                    AtomicInteger count = new AtomicInteger(0);
                    UtilRisc.forEach(threadRun, ServicePromise.queueMultipleCompleteSet, promise -> {
                        assert promise != null;
                        promise.completePromise();
                        count.incrementAndGet();
                    });
                });
    }

}
