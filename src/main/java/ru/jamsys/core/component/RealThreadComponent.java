package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.promise.PromiseTask;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class RealThreadComponent implements Resource<PromiseTask, Void> {

    @Override
    public Void execute(PromiseTask arguments) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 1;
    }

}
