package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.broker.persist.BrokerPersist;
import ru.jamsys.core.extension.broker.persist.X;
import ru.jamsys.core.extension.broker.persist.element.StatisticElement;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Component
@Lazy
public class StatisticUpload extends PromiseGenerator implements Cron1s {

    private final ServicePromise servicePromise;

    private final StatisticFlush serviceFlush;

    private final StatisticUploadRepositoryProperty property = new StatisticUploadRepositoryProperty();

    public StatisticUpload(ServicePromise servicePromise, StatisticFlush serviceFlush) {
        this.servicePromise = servicePromise;
        this.serviceFlush = serviceFlush;
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
                .appendWithResource("main", HttpResource.class, (threadRun, promiseTask, promise, resource) -> {
//                    HttpConnectorDefault httpConnectorDefault = new HttpConnectorDefault()
//                            .setUrl("");
//                    HttpResponse execute = resource.execute(httpConnectorDefault);
                })
                .append("main", (threadRun, _, _) -> {
                    BrokerPersist<StatisticElement> statisticElementBrokerPersist =
                            serviceFlush.getBrokerPersistManagerConfiguration().get();
                    int batch = property.getBatchMaxSizeByte();
                    List<X<StatisticElement>> list = new ArrayList<>();
                    while (batch > 0) {
                        X<StatisticElement> poll = statisticElementBrokerPersist.poll();
                        if (poll == null) {
                            break;
                        }
                        list.add(poll);
                        batch--;
                    }
                    StringBuilder sb = new StringBuilder();
                    list.forEach(statisticElementX -> sb.append(statisticElementX.getElement().getValue()));

                    list.forEach(statisticElementBrokerPersist::commit);
                });
    }

}
