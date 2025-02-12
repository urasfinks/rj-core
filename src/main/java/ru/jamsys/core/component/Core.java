package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.ManagerFileByteWriter;
import ru.jamsys.core.component.manager.item.FileByteWriter;
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.UniqueClassNameImpl;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.statistic.StatisticSec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
@Lazy
public class Core implements LifeCycleInterface {

    private final ServiceClassFinder serviceClassFinder;

    private final ApplicationContext applicationContext;

    private final ManagerFileByteWriter managerFileByteWriter;

    private final ManagerBroker managerBroker;

    public static String lastOperation;

    private final ConcurrentLinkedDeque<LifeCycleComponent> runComponent = new ConcurrentLinkedDeque<>();

    public Core(
            ApplicationContext applicationContext,
            ServiceClassFinder serviceClassFinder,
            ManagerFileByteWriter managerFileByteWriter,
            ManagerBroker managerBroker
    ) {
        this.applicationContext = applicationContext;
        this.serviceClassFinder = serviceClassFinder;
        this.managerFileByteWriter = managerFileByteWriter;
        this.managerBroker = managerBroker;
    }

    @Override
    public void run() {
        Util.logConsole(getClass(), ".run()");
        String indexStatistic = UniqueClassNameImpl.getClassNameStatic(StatisticSec.class, null, applicationContext);
        String indexLog = UniqueClassNameImpl.getClassNameStatic(Log.class, null, applicationContext);

        FileByteWriter fileByteWriterStatistic = managerFileByteWriter.get(indexStatistic);
        FileByteWriter fileByteWriterLog = managerFileByteWriter.get(indexLog);

        managerBroker.initAndGet(indexStatistic, StatisticSec.class, fileByteWriterStatistic::append);
        managerBroker.initAndGet(indexLog, Log.class, fileByteWriterLog::append);

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
            lifeCycleComponent.run();
            Util.logConsole(
                    getClass(),
                    "run index: " + lifeCycleComponent.getInitializationIndex()
                            + "; class: " + lifeCycleComponent.getClass().getName()
                            + "; time: " + (System.currentTimeMillis() - start) + "ms"
            );
        });
    }

    @Override
    public void shutdown() {
        Util.logConsole(getClass(), ".shutdown()");
        while (!runComponent.isEmpty()) {
            LifeCycleComponent lifeCycleComponent = runComponent.pollLast();
            if (lifeCycleComponent != null) {
                lastOperation = lifeCycleComponent.getClass().getName();
                long start = System.currentTimeMillis();
                lifeCycleComponent.shutdown();
                Util.logConsole(
                        getClass(),
                        "shutdown index: " + lifeCycleComponent.getInitializationIndex()
                                + "; class: " + lifeCycleComponent.getClass().getName()
                                + "; time: " + (System.currentTimeMillis() - start) + "ms"

                );
            }
        }
    }

}
