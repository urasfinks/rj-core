package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;

import java.util.function.Function;

@Component
@Lazy
public class ServiceThreadReal implements Resource<PromiseTask, Void> {

    @Override
    public void constructor(NamespaceResourceConstructor constructor) {

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
