package ru.jamsys.core.component;

import org.springframework.stereotype.Component;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.resource.balancer.algorithm.LeastConnections;

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

@Component
public class VirtualThreadComponent implements Resource<Void, PromiseTask, Void> {

    private final ExecutorService executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("v-thread-", 0).factory());

    private final AtomicBoolean isThreadRun = new AtomicBoolean(true);

    @Override
    public void constructor(Void constructor) {

    }

    @Override
    public Void execute(PromiseTask arguments) {
        arguments.setIsThreadRun(isThreadRun);
        executorService.submit(arguments);
        return null;
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        if (balancerAlgorithm instanceof LeastConnections) {
            return ((ThreadPoolExecutor) executorService).getActiveCount();
        }
        return isThreadRun.get() ? 1 : 0;
    }

}
