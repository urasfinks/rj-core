package ru.jamsys.core.component.cron;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.ByteItem;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.extension.property.PropertyConnector;
import ru.jamsys.core.extension.property.PropertyName;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.flat.template.cron.release.Cron5s;
import ru.jamsys.core.flat.util.ListSort;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.filebyte.reader.FileByteReaderRequest;
import ru.jamsys.core.resource.filebyte.reader.FileByteReaderResource;
import ru.jamsys.core.resource.influx.InfluxResource;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.StatisticSec;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@Lazy
public class StatisticUploader extends PropertyConnector implements Cron5s, PromiseGenerator, ClassName {

    final Broker<StatisticSec> broker;

    @Setter
    private String index;

    private final ServicePromise servicePromise;

    private final Subscriber subscriber;

    @Getter
    @PropertyName("default.log.file.folder")
    private String folder = "LogManager";

    public StatisticUploader(
            ManagerBroker managerBroker,
            ApplicationContext applicationContext,
            ServicePromise servicePromise,
            ServiceProperty serviceProperty
    ) {
        this.servicePromise = servicePromise;
        broker = managerBroker.get(
                ClassNameImpl.getClassNameStatic(StatisticSec.class, null, applicationContext),
                StatisticSec.class
        );
        subscriber = serviceProperty.getSubscriber(
                null,
                this,
                getClassName(applicationContext),
                false
        );
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 2_000L)
                .appendWithResource("sendToInflux", InfluxResource.class, (isThreadRun, promise, influxResource) -> {
                    List<Point> listPoints = new ArrayList<>();
                    List<StatisticSec> reserve = new ArrayList<>();
                    while (!broker.isEmpty() && isThreadRun.get()) {
                        ExpirationMsImmutableEnvelope<StatisticSec> envelope = broker.pollFirst();
                        if (envelope != null) {
                            StatisticSec statisticSec = envelope.getValue();
                            reserve.add(statisticSec);
                            List<Statistic> list = statisticSec.getList();
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
                    if (!reserve.isEmpty()) {
                        promise.setProperty("reserve", reserve);
                    }
                    //syslog,
                    // appname=mysecondapp,
                    // facility=console,
                    // host=myhost,
                    // hostname=myhost,
                    // severity=crit
                    // facility_code=14i,
                    // message="critical message here",
                    // severity_code=2i,procid="12346"
                    // ,timestamp=1534418426076078000i,
                    // version=1i
//                    listPoints.add(
//                            Point.measurement("TestLog")
//                                    .addTag("correlation", java.util.UUID.randomUUID().toString())
//                                    .addTag("severity", "err")
//                                    .addField("message", "[ERROR] " + "1")
//                                    .time(System.currentTimeMillis(), WritePrecision.MS)
//
//                    );
//
//                    listPoints.add(
//                            Point.measurement("TestLog")
//                                    .addTag("correlation", java.util.UUID.randomUUID().toString())
//                                    .addTag("severity", "info")
//                                    .addField("message", "[INFO] " + "2")
//                                    .time(System.currentTimeMillis(), WritePrecision.MS)
//
//                    );
//
//                    listPoints.add(
//                            Point.measurement("TestLog")
//                                    .addTag("correlation", java.util.UUID.randomUUID().toString())
//                                    .addTag("severity", "debug")
//                                    .addField("message", "[DEBUG] " + "3")
//                                    .time(System.currentTimeMillis(), WritePrecision.MS)
//
//                    );
                    influxResource.execute(listPoints);
                })
                .then("readDirectory", (_, promise) -> {
                    List<String> filesRecursive = UtilFile.getFilesRecursive(getFolder(), false);
                    List<String> restore = new ArrayList<>();
                    for (String filePath : filesRecursive) {
                        if (filePath.startsWith("/statistic.") && !filePath.endsWith(".proc.bin")) {
                            restore.add(filePath);
                        }
                    }
                    if (!restore.isEmpty()) {
                        promise.setProperty("readyFile", getFolder() + ListSort.sort(restore).getFirst());
                    }
                })
                .appendWait()
                .appendWithResource("read", FileByteReaderResource.class, (_, promise, fileByteReaderResource) -> {
                    if (broker.getOccupancyPercentage() < 50) {
                        String readyFile = promise.getProperty("readyFile", String.class);
                        if (readyFile != null) {
                            List<ByteItem> execute = fileByteReaderResource.execute(
                                    new FileByteReaderRequest(readyFile, StatisticSec.class)
                            );
                            execute.forEach(byteItem -> broker.add((StatisticSec) byteItem, 6_000L));
                            try {
                                UtilFile.remove(readyFile);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                })
                .onError((_, promise) -> {
                    List<StatisticSec> reserve = promise.getProperty("reserve", List.class, null);
                    if (reserve != null && !reserve.isEmpty()) {
                        reserve.forEach(statisticSec -> broker.add(statisticSec, 2_000L));
                    }
                    //System.out.println(promise.getLog());
                });
    }

}
