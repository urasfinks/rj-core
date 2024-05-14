package ru.jamsys.core.component.resource;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.extension.Resource;
import ru.jamsys.core.promise.PromiseTask;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class RealThreadComponent implements Resource<Void, PromiseTask> {

    @Override
    public Void execute(PromiseTask arguments) {
        return null;
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 1;
    }
}
