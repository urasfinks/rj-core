package ru.jamsys.core.component.resource;

import org.springframework.stereotype.Component;
import ru.jamsys.core.promise.PromiseTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wow it's amazing!
 *
 * <p>
 * <pre> {@code
 * int x = x + 1;
 * }</pre>
 */

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class VirtualThreadComponent {

    private final ExecutorService executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("v-thread-", 0).factory());
    private final AtomicBoolean isThreadRun = new AtomicBoolean(true);

    public void submit(PromiseTask task) {
        task.setIsThreadRun(isThreadRun);
        executorService.submit(task);
    }

}
