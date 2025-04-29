package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.flat.util.UtilLog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
@Lazy
public class Core extends AbstractLifeCycle implements LifeCycleInterface {

    private final ServiceClassFinder serviceClassFinder;

    public static String lastOperation;

    private final ConcurrentLinkedDeque<LifeCycleComponent> runComponent = new ConcurrentLinkedDeque<>();

    public Core(ServiceClassFinder serviceClassFinder) {
        this.serviceClassFinder = serviceClassFinder;
    }

    @Override
    public void runOperation() {
        UtilLog.printInfo(getClass(), "run()");
        List<LifeCycleComponent> sortedList = new ArrayList<>();
        serviceClassFinder.findByInstance(LifeCycleComponent.class).forEach((Class<LifeCycleComponent> runnableComponentClass) -> {
            if (!ServiceClassFinder.instanceOf(this.getClass(), runnableComponentClass)) {
                sortedList.add(serviceClassFinder.instanceOf(runnableComponentClass));
            }
        });
        sortedList.sort(Comparator.comparingInt(LifeCycleComponent::getInitializationIndex));
        sortedList.forEach(lifeCycleComponent -> {
            runComponent.add(lifeCycleComponent);
            long start = System.currentTimeMillis();
            ResultOperation resultOperation = lifeCycleComponent.run();
            UtilLog.info(getClass(), null)
                    .addHeader("runIndex", lifeCycleComponent.getInitializationIndex())
                    .addHeader("runClass", lifeCycleComponent.getInitializationIndex())
                    .addHeader("runTime", (System.currentTimeMillis() - start) + "ms")
                    .addHeader("resultOperation", resultOperation)
                    .print();
        });
    }

    @Override
    public void shutdownOperation() {
        UtilLog.printInfo(getClass(), "shutdown()");
        while (!runComponent.isEmpty()) {
            LifeCycleComponent lifeCycleComponent = runComponent.pollLast();
            if (lifeCycleComponent != null) {
                lastOperation = lifeCycleComponent.getClass().getName();
                long start = System.currentTimeMillis();
                ResultOperation resultOperation = lifeCycleComponent.shutdown();
                UtilLog.info(getClass(), null)
                        .addHeader("shutdownIndex", lifeCycleComponent.getInitializationIndex())
                        .addHeader("shutdownClass", lifeCycleComponent.getClass().getName())
                        .addHeader("shutdownTime", (System.currentTimeMillis() - start) + "ms")
                        .addHeader("resultOperation", resultOperation)
                        .print();
            }
        }
    }

}
