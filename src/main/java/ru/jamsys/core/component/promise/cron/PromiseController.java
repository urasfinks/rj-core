package ru.jamsys.core.component.promise.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskType;
import ru.jamsys.core.template.cron.release.Cron1s;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class PromiseController implements Cron1s, PromiseGenerator {

    @Override
    public Promise generate() {
        return new PromiseImpl(getClass().getName())
                .append(this.getClass().getName(), PromiseTaskType.IO, (AtomicBoolean isThreadRun) -> {
                    int count = 0;
                    while (!PromiseImpl.queueMultipleComplete.isEmpty() && isThreadRun.get()) {
                        Promise promise = PromiseImpl.queueMultipleComplete.pollFirst();
                        assert promise != null;
                        promise.complete();
                        count++;
                    }
                    if (count > 0) {
                        App.context.getBean(ExceptionHandler.class).handler(new RuntimeException("Multiple complete: " + count));
                    }
                });
    }

}
