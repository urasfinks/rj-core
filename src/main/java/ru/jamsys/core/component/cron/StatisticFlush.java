package ru.jamsys.core.component.cron;

import com.influxdb.client.write.Point;
import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.broker.persist.BrokerPersist;
import ru.jamsys.core.extension.broker.persist.element.StatisticElement;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;
import ru.jamsys.core.extension.victoria.metrics.VictoriaMetricsConvert;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Component
@Lazy
@Getter
public class StatisticFlush extends PromiseGenerator implements Cron1s {

    List<StatisticsFlushComponent> list = new ArrayList<>();

    String host = Util.getHostname();

    private final ServicePromise servicePromise;

    private final ManagerConfiguration<BrokerPersist<StatisticElement>> brokerPersistManagerConfiguration;

    public StatisticFlush(
            ServiceClassFinder serviceClassFinder,
            ServicePromise servicePromise,
            ServiceProperty serviceProperty
    ) {
        this.servicePromise = servicePromise;
        serviceClassFinder.findByInstance(StatisticsFlushComponent.class).forEach(statisticsCollectorClass
                -> list.add(App.get(statisticsCollectorClass)));
        brokerPersistManagerConfiguration = ManagerConfiguration.getInstance(
                BrokerPersist.class,
                App.getUniqueClassName(StatisticFlush.class),
                "statistic",
                managerElement -> managerElement
                        .setup((bytes) -> new StatisticElement(new String(bytes)))
        );
    }

    @Override
    public boolean isTimeHasCome(Cron.CompileResult compileResult) {
        // Запускать будем спустя секунду, а не прямо в момент старта
        // Это влияет на тесты
        return compileResult.getBeforeTimestamp() != 0;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(App.getUniqueClassName(getClass()), 6_000L)
                .appendIO("main", (threadRun, _, _) -> {
                    StringBuilder sb = new StringBuilder();
                    for (StatisticsFlushComponent statisticsFlushComponent : list) {
                        for (StatisticDataHeader statisticDataHeader : statisticsFlushComponent.flushAndGetStatistic(threadRun)) {
                            Point influxPoint = VictoriaMetricsConvert.getInfluxFormat(statisticDataHeader);
                            influxPoint.addTag("host", host);
                            sb.append(influxPoint.toLineProtocol()).append("\n");
                        }
                    }
                    brokerPersistManagerConfiguration.get().add(new StatisticElement(sb.toString()));
                    // После изучения проблемы ниже, было выявленно, что хранить некорректную ссылку невозможно
                    // ManagerConfiguration.get() возвращает объект напрямую из Manager а там всё сделано на concurrent.
                    // Проблема в гонке получения get() и одновременном Manager.helper() -> shutdown().
                    // Это история должна быть разовой

                    // TODO: [
                    //  "2025-09-06T12:47:08.006 v-thread-141353",
                    //  "ru.jamsys.core.extension.exception.ForwardException: ru.jamsys.core.extension.exception.ForwardException: java.lang.RuntimeException: Writer is closed",
                    //  "ForwardException.Context:",
                    //  "{",
                    //  "  \"addTime\" : \"2025-09-06T12:47:08.006\",",
                    //  "  \"expiration\" : \"2025-09-06T12:47:14.006\",",
                    //  "  \"diffTime\" : 0,",
                    //  "  \"ns\" : \"StatisticFlush\",",
                    //  "  \"run\" : true,",
                    //  "  \"terminalStatus\" : \"IN_PROCESS\",",
                    //  "  \"trace\" : [",
                    //  "    \"2025-09-06T12:47:08.006 StatisticFlush::run()\",",
                    //  "    \"2025-09-06T12:47:08.006 StatisticFlush.main::run()\"",
                    //  "  ]",
                    //  "}",
                    //  "   at ru.jamsys.core.promise.Promise.setError(Promise.java:204)",
                    //  "   ... 4 more",
                    //  "Caused by: ",
                    //  "ru.jamsys.core.extension.exception.ForwardException: java.lang.RuntimeException: Writer is closed",
                    //  "ForwardException.Context:",
                    //  "{",
                    //  "  \"hashCode\" : \"5553e195\",",
                    //  "  \"cls\" : \"ru.jamsys.core.promise.PromiseTask\",",
                    //  "  \"ns\" : \"StatisticFlush.main\",",
                    //  "  \"type\" : \"IO\"",
                    //  "}",
                    //  "   at ru.jamsys.core.promise.AbstractPromiseTask.run(AbstractPromiseTask.java:145)",
                    //  "   ... 3 more",
                    //  "Caused by: ",
                    //  "java.lang.RuntimeException: Writer is closed",
                    //  "   at ru.jamsys.core.extension.async.writer.AsyncFileWriterRolling.writeAsync(AsyncFileWriterRolling.java:67)",
                    //  "   at ru.jamsys.core.extension.broker.persist.BrokerPersist.add(BrokerPersist.java:292)",
                    //  "   at ru.jamsys.core.component.cron.StatisticFlush.lambda$generate$3(StatisticFlush.java:76)",
                    //  "   at ru.jamsys.core.promise.AbstractPromiseTask.executeProcedure(AbstractPromiseTask.java:111)",
                    //  "   at ru.jamsys.core.promise.AbstractPromiseTask.run(AbstractPromiseTask.java:131)",
                    //  "   at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:572)",
                    //  "   at java.util.concurrent.FutureTask.run(FutureTask.java:317)",
                    //  "   at java.lang.VirtualThread.run(VirtualThread.java:329)"
                    //]
                });
    }

}
