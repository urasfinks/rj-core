package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.promise.PromiseTask;

import java.util.function.Function;

@Component
@Lazy
public class ServiceThreadReal implements Resource<Void, PromiseTask, Void> {

    @Override
    public void constructor(Void constructor) {

    }

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

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

}
