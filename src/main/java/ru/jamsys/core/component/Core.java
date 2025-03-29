package ru.jamsys.core.component;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.ManagerFileByteWriter;
import ru.jamsys.core.component.manager.item.log.PersistentData;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.broker.persist.BrokerMemory;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.statistic.StatisticSec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class Core implements LifeCycleInterface {

    private final ServiceClassFinder serviceClassFinder;

    private final ManagerFileByteWriter managerFileByteWriter;

    private final ManagerBroker managerBroker;

    public static String lastOperation;

    private final AtomicBoolean run = new AtomicBoolean(false);

    private final ConcurrentLinkedDeque<LifeCycleComponent> runComponent = new ConcurrentLinkedDeque<>();

    @Getter
    private BrokerMemory<StatisticSec> statisticSecBroker;

    @Getter
    private BrokerMemory<PersistentData> logBroker;

    public Core(
            ServiceClassFinder serviceClassFinder,
            ManagerFileByteWriter managerFileByteWriter,
            ManagerBroker managerBroker
    ) {
        this.serviceClassFinder = serviceClassFinder;
        this.managerFileByteWriter = managerFileByteWriter;
        this.managerBroker = managerBroker;
    }

    @Override
    public boolean isRun() {
        return run.get();
    }

    @Override
    public void run() {
        run.set(true);
        UtilLog.info(getClass(), null).addHeader("description", "run");
        // Это работает так: инициализируем 2 очереди для логов и статистики
        // Планируем, что из этих очередей будет своевременно вычитывать обещания из крона,
        // которые должны сгружать их куда-то далеко удалённо
        // Если обещания своевременно не сгружают логи/стату - сообщения начинают протухать
        // и тут эти сообщения сливаются в onDrop в Файловые писальщики, которые должны их записывать на ФС
        // Очереди Broker и FileByteWriter - разные
        statisticSecBroker = managerBroker.initAndGet(
                App.getUniqueClassName(StatisticSec.class),
                StatisticSec.class,
                managerFileByteWriter.get(
                        App.getUniqueClassName(StatisticSec.class),
                        StatisticSec.class
                )::append
        );
        logBroker = managerBroker.initAndGet(
                App.getUniqueClassName(PersistentData.class),
                PersistentData.class,
                managerFileByteWriter.get(
                        App.getUniqueClassName(PersistentData.class),
                        PersistentData.class
                )::append
        );

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
            UtilLog.info(getClass(), null)
                    .addHeader("runIndex", lifeCycleComponent.getInitializationIndex())
                    .addHeader("runClass", lifeCycleComponent.getInitializationIndex())
                    .addHeader("runTime", (System.currentTimeMillis() - start) + "ms")
                    .print();
        });
    }

    @Override
    public void shutdown() {
        UtilLog.printInfo(getClass(), "shutdown()");
        while (!runComponent.isEmpty()) {
            LifeCycleComponent lifeCycleComponent = runComponent.pollLast();
            if (lifeCycleComponent != null) {
                lastOperation = lifeCycleComponent.getClass().getName();
                long start = System.currentTimeMillis();
                lifeCycleComponent.shutdown();
                UtilLog.info(getClass(), null)
                        .addHeader("shutdownIndex", lifeCycleComponent.getInitializationIndex())
                        .addHeader("shutdownClass", lifeCycleComponent.getClass().getName())
                        .addHeader("shutdownTime", (System.currentTimeMillis() - start) + "ms")
                        .print();
            }
        }
        run.set(false);
    }

}
