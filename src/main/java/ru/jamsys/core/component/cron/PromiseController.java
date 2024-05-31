package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseImpl;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Lazy
public class PromiseController implements Cron1s, PromiseGenerator {

    @Override
    public Promise generate() {
        return new PromiseImpl(getClass().getName(),6_000L)
                .append(this.getClass().getName(), (AtomicBoolean isThreadRun, Promise _) -> {
                    AtomicInteger count = new AtomicInteger(0);
                    UtilRisc.forEach(isThreadRun, PromiseImpl.queueMultipleCompleteSet, promise -> {
                        assert promise != null;
                        promise.complete();
                        count.incrementAndGet();
                    });

                    if (count.get() > 0) {
                        App.context.getBean(ExceptionHandler.class).handler(new RuntimeException("Multiple complete: " + count));
                    }
                });
    }

}
