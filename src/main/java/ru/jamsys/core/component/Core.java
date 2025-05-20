package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.expiration.ExpirationList;
import ru.jamsys.core.extension.expiration.ExpirationMap;
import ru.jamsys.core.extension.expiration.MapRemover;
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

    private final Manager manager;

    public Core(ServiceClassFinder serviceClassFinder, Manager manager) {
        this.serviceClassFinder = serviceClassFinder;
        this.manager = manager;
    }

    public static Manager.Configuration<ExpirationList<MapRemover>> expirationMapConfiguration;

    @Override
    public void runOperation() {
        UtilLog.printAction("run()");
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
            UtilLog.info(null)
                    .addHeader("runIndex", lifeCycleComponent.getInitializationIndex())
                    .addHeader("runClass", lifeCycleComponent.getClass().getName())
                    .addHeader("runTime", (System.currentTimeMillis() - start) + "ms")
                    .addHeader("resultOperation", resultOperation)
                    .print();
        });
        // Обслуживание всех ExpirationMap
        expirationMapConfiguration = manager.configureGeneric(
                ExpirationList.class,
                ExpirationMap.class.getName(),
                s -> new ExpirationList<>(s, disposableExpirationMsImmutableEnvelope -> {
                    MapRemover value = disposableExpirationMsImmutableEnvelope.getValue();
                    if (value != null) {
                        value.remove();
                    }
                })
        );
    }

    @Override
    public void shutdownOperation() {
        UtilLog.printAction("shutdown()");
        while (!runComponent.isEmpty()) {
            LifeCycleComponent lifeCycleComponent = runComponent.pollLast();
            if (lifeCycleComponent != null) {
                lastOperation = lifeCycleComponent.getClass().getName();
                long start = System.currentTimeMillis();
                ResultOperation resultOperation = lifeCycleComponent.shutdown();
                UtilLog.info( null)
                        .addHeader("shutdownIndex", lifeCycleComponent.getInitializationIndex())
                        .addHeader("shutdownClass", lifeCycleComponent.getClass().getName())
                        .addHeader("shutdownTime", (System.currentTimeMillis() - start) + "ms")
                        .addHeader("resultOperation", resultOperation)
                        .print();
            }
        }
    }

}
