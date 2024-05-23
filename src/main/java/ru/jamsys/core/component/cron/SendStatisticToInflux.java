package ru.jamsys.core.component.cron;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.EnvelopManagerObject;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.flat.template.cron.release.Cron5s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskExecuteType;
import ru.jamsys.core.promise.resource.api.InfluxClientPromise;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.StatisticSec;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class SendStatisticToInflux implements Cron5s, PromiseGenerator, ClassName {

    final EnvelopManagerObject<Broker<StatisticSec>> brokerEnvelopManagerObject;

    public SendStatisticToInflux(BrokerManager brokerManager, ApplicationContext applicationContext) {
        brokerEnvelopManagerObject = brokerManager.get(ClassNameImpl.getClassNameStatic(StatisticSec.class, null, applicationContext), StatisticSec.class);
    }

    @Override
    public Promise generate() {
        Promise promise = new PromiseImpl(getClassName(), 6_000L);
        return promise;
    }

    public Promise generateOld() {
        //TODO: replace ::collector IO -> COMPUTE (в текущий момент нет реализации COMPUTE)
        Promise promise = new PromiseImpl(getClass().getName(), 6_000L);
        promise.append(getClassName("collector"), PromiseTaskExecuteType.IO, (AtomicBoolean isThreadRun) -> {
                    brokerEnvelopManagerObject.accept(queue -> {
                        List<Point> listPoints = new ArrayList<>();
                        while (!queue.isEmpty() && isThreadRun.get()) {
                            ExpirationMsImmutableEnvelope<StatisticSec> statisticSec = queue.pollFirst();
                            if (statisticSec != null) {
                                List<Statistic> list = statisticSec.getValue().getList();
                                for (Statistic statistic : list) {
                                    HashMap<String, String> newTags = new HashMap<>(statistic.getTags());
                                    String measurement = newTags.remove("measurement");
                                    listPoints.add(
                                            Point.measurement(measurement)
                                                    .addTags(newTags)
                                                    .addFields(statistic.getFields())
                                                    .time(statisticSec.getLastActivityMs(), WritePrecision.MS)
                                    );
                                }
                            }
                        }
                        promise.setProperty("preparePoint", listPoints);
                    });
                })
                .waits()
                .api(getClassName("sendToInflux"), new InfluxClientPromise().beforeExecute((InfluxClientPromise influxClientPromise) -> {
                    @SuppressWarnings("unchecked")
                    List<Point> list = (List<Point>) promise.getProperty("preparePoint", List.class);
                    influxClientPromise.getList().addAll(list);
                }));
        return promise;
    }
}
