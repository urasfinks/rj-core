package ru.jamsys.core.component;

import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.LifeCycleComponent;
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

@Component
public class ServiceThreadVirtual implements LifeCycleComponent {

    private final ExecutorService executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("v-thread-", 0).factory());

    private final AtomicBoolean run = new AtomicBoolean(true);

    public void execute(PromiseTask arguments) {
        arguments.setThreadRun(run);
        executorService.submit(arguments);
    }

    @Override
    public int getInitializationIndex() {
        return 1;
    }

    @Override
    public void run() {

    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

}
