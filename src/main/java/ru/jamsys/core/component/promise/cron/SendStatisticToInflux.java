package ru.jamsys.core.component.promise.cron;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.api.BrokerManager;
import ru.jamsys.core.component.item.Broker;
import ru.jamsys.core.component.promise.api.InfluxClientPromise;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskType;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.StatisticSec;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;
import ru.jamsys.core.template.cron.release.Cron5s;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class SendStatisticToInflux implements Cron5s, PromiseGenerator, ClassName {

    final BrokerManager<StatisticSec> broker;

    public SendStatisticToInflux(BrokerManager<StatisticSec> broker) {
        this.broker = broker;
    }

    @Override
    public Promise generate() {
        Promise promise = new PromiseImpl(getClassName());
        return promise;
    }

    public Promise generateOld() {
        //TODO: replace ::collector IO -> COMPUTE (в текущий момент нет реализации COMPUTE)
        Promise promise = new PromiseImpl(getClass().getName());
        promise.append(getClass().getName() + "::collector", PromiseTaskType.IO, (AtomicBoolean isThreadRun) -> {
                    Broker<StatisticSec> queue = broker.get(ClassNameImpl.getClassNameStatic(StatisticSec.class, null));
                    List<Point> listPoints = new ArrayList<>();
                    while (!queue.isEmpty() && isThreadRun.get()) {
                        TimeEnvelopeMs<StatisticSec> statisticSec = queue.pollFirst();
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
                    promise.getProperty().put("preparePoint", listPoints);
                })
                .waits()
                .api(getClass().getName() + "::sendToInflux", new InfluxClientPromise().beforeExecute((InfluxClientPromise influxClientPromise) -> {
                    @SuppressWarnings("unchecked")
                    List<Point> list = (List<Point>) promise.getProperty().get("preparePoint");
                    influxClientPromise.getList().addAll(list);
                }));
        return promise;
    }
}
