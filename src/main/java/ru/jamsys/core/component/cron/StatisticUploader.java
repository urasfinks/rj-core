package ru.jamsys.core.component.cron;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.ByteItem;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.extension.property.PropertyConnector;
import ru.jamsys.core.extension.property.PropertyName;
import ru.jamsys.core.flat.template.cron.release.Cron5s;
import ru.jamsys.core.flat.util.ListSort;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.PoolSettingsRegistry;
import ru.jamsys.core.resource.filebyte.reader.FileByteReaderRequest;
import ru.jamsys.core.resource.filebyte.reader.FileByteReaderResource;
import ru.jamsys.core.resource.influx.InfluxResource;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.StatisticSec;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Component
@Lazy
public class StatisticUploader extends PropertyConnector implements Cron5s, PromiseGenerator, ClassName {

    final Broker<StatisticSec> broker;

    @Setter
    private String index;

    private final ServicePromise servicePromise;

    @Getter
    @PropertyName("default.log.file.folder")
    private String folder = "LogManager";

    @Getter
    @PropertyName("run.args.remote.statistic.limit.points")
    private String limitInsert = "10000";

    public enum StatisticUploaderPromiseProperty {
        RESERVE_STATISTIC,
    }

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
        serviceProperty.getSubscriber(
                null,
                this,
                null,
                false
        );
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 4_999L)
                .append("checkStatistic", (_, promise) -> {
                    if (broker.isEmpty()) {
                        promise.skipAllStep();
                    }
                })
                .thenWithResource("sendToInflux", InfluxResource.class, (isThreadRun, promise, influxResource) -> {
                    int limitInsert = Integer.parseInt(this.limitInsert);
                    AtomicInteger countInsert = new AtomicInteger(0);
                    List<Point> listPoints = new ArrayList<>();

                    List<StatisticSec> reserve = new ArrayList<>();
                    while (!broker.isEmpty() && isThreadRun.get() && countInsert.get() < limitInsert) {
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
                                countInsert.incrementAndGet();
                            }
                        }
                    }
                    promise.setProperty(StatisticUploaderPromiseProperty.RESERVE_STATISTIC.name(), reserve);

                    if (countInsert.get() > 0) {
                        influxResource.execute(listPoints);
                    }
                })
                .then("readDirectory", (_, promise) -> {
                    String indexStatistic = ClassNameImpl.getClassNameStatic(StatisticSec.class, null, App.context);
                    List<String> filesRecursive = UtilFile.getFilesRecursive(getFolder(), false);
                    List<String> restore = new ArrayList<>();
                    for (String filePath : filesRecursive) {
                        if (filePath.startsWith("/" + indexStatistic + ".") && !filePath.endsWith(".proc.bin")) {
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
                    Throwable exception = promise.getException();
                    if (exception != null) {
                        @SuppressWarnings("unchecked")
                        Function<Throwable, Boolean> isFatalExceptionOnComplete = App
                                .get(PoolSettingsRegistry.class)
                                .get(InfluxResource.class, "default")
                                .getFunctionCheckFatalException();
                        if (isFatalExceptionOnComplete.apply(exception)) {
                            // Уменьшили срок с 6сек до 2сек, что бы при падении Influx быстрее сгрузить данные на файловую систему
                            List<StatisticSec> reserveStatistic = promise.getProperty(StatisticUploaderPromiseProperty.RESERVE_STATISTIC.name(), List.class, null);
                            if (reserveStatistic != null && !reserveStatistic.isEmpty()) {
                                reserveStatistic.forEach(statisticSec -> broker.add(statisticSec, 2_000L));
                            }
                        }
                    }
                });
    }

}
