package ru.jamsys.core.component.resource;

import org.springframework.stereotype.Component;
import ru.jamsys.core.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.balancer.algorithm.LeastConnections;
import ru.jamsys.core.extension.Resource;
import ru.jamsys.core.promise.PromiseTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
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
public class VirtualThreadComponent implements Resource<Void, PromiseTask> {

    private final ExecutorService executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("v-thread-", 0).factory());
    private final AtomicBoolean isThreadRun = new AtomicBoolean(true);

    @Override
    public Void execute(PromiseTask arguments) {
        arguments.setIsThreadRun(isThreadRun);
        executorService.submit(arguments);
        return null;
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        if (balancerAlgorithm instanceof LeastConnections) {
            return ((ThreadPoolExecutor) executorService).getActiveCount();
        }
        return isThreadRun.get() ? 1 : 0;
    }

}
