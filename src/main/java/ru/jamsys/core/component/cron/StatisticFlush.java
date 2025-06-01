package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Component
@Lazy
public class StatisticFlush extends PromiseGenerator implements Cron1s {

    List<StatisticsFlushComponent> list = new ArrayList<>();

    String ip = Util.getIp();

    private final ServicePromise servicePromise;

    public StatisticFlush(
            ServiceClassFinder serviceClassFinder,
            ServicePromise servicePromise,
            ServiceProperty serviceProperty
    ) {
        this.servicePromise = servicePromise;
        serviceClassFinder.findByInstance(StatisticsFlushComponent.class).forEach(statisticsCollectorClass
                -> list.add(App.get(statisticsCollectorClass)));
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
                .append("main", (threadRun, _, _) -> {
                    UtilRisc.forEach(threadRun, list, (StatisticsFlushComponent statisticsFlushComponent) -> {
                        List<DataHeader> listStatistic = statisticsFlushComponent.flushAndGetStatistic(threadRun);
//                        Map<String, String> parentTags = new LinkedHashMap<>();
//                        String measurement = App.getUniqueClassName(statisticsFlushComponent.getClass());
//                        parentTags.put("measurement", measurement);
//                        parentTags.put("Host", ip);
//                        List<Statistic> statistics = statisticsFlushComponent.flushAndGetStatistic(
//                                parentTags,
//                                null,
//                                threadRun
//                        );
//                        if (statistics != null && !statistics.isEmpty()) {
//                            statisticSec.getList().addAll(statistics);
//                        }
                    });
                    // Несмотря на remoteStatistic надо с сервисов сбрасывать статистику
                    // Так что мы будем всё собирать, но отправлять не будем
//                    if (!statisticSec.getList().isEmpty() && remote) {
//                        broker.add(new ExpirationMsImmutableEnvelope<>(statisticSec, 6_000));
//                    }
                });
    }

}
