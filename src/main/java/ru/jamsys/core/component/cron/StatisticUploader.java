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
import ru.jamsys.core.extension.ByteTransformer;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.extension.UniqueClassNameImpl;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
import ru.jamsys.core.extension.annotation.PropertyName;
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
public class StatisticUploader extends RepositoryPropertiesField implements Cron5s, PromiseGenerator, UniqueClassName {

    final Broker<StatisticSec> broker;

    @Setter
    @Getter
    private String index;

    private final ServicePromise servicePromise;

    @Getter
    @PropertyName("default.log.file.folder")
    private String folder;

    @Getter
    @PropertyName("run.args.remote.statistic.limit.points")
    private Integer limitInsert;

    @Getter
    @PropertyName("run.args.remote.statistic")
    private Boolean remoteStatistic;

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
                UniqueClassNameImpl.getClassNameStatic(StatisticSec.class, null, applicationContext),
                StatisticSec.class
        );
        serviceProperty.getFactory().getPropertiesAgent(
                null,
                this,
                null,
                false
        );
    }

    @Override
    public Promise generate() {
        if (!remoteStatistic) {
            return null;
        }
        return servicePromise.get(index, 4_999L)
                .append("checkStatistic", (_, promise) -> {
                    if (broker.isEmpty()) {
                        promise.skipAllStep();
                    }
                })
                .thenWithResource("sendToInflux", InfluxResource.class, (isThreadRun, promise, influxResource) -> {
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
                    promise.setMapRepository(StatisticUploaderPromiseProperty.RESERVE_STATISTIC.name(), reserve);

                    if (countInsert.get() > 0) {
                        influxResource.execute(listPoints);
                    }
                })
                .then("readDirectory", (_, promise) -> {
                    String indexStatistic = UniqueClassNameImpl.getClassNameStatic(StatisticSec.class, null, App.context);
                    List<String> filesRecursive = UtilFile.getFilesRecursive(getFolder(), false);
                    List<String> restore = new ArrayList<>();
                    for (String filePath : filesRecursive) {
                        if (filePath.startsWith("/" + indexStatistic + ".") && !filePath.endsWith(".proc.bin")) {
                            restore.add(filePath);
                        }
                    }
                    if (!restore.isEmpty()) {
                        promise.setMapRepository("readyFile", getFolder() + ListSort.sortAsc(restore).getFirst());
                    }
                })
                .appendWait()
                .appendWithResource("read", FileByteReaderResource.class, (_, promise, fileByteReaderResource) -> {
                    if (broker.getOccupancyPercentage() < 50) {
                        String readyFile = promise.getRepositoryMap("readyFile", String.class);
                        if (readyFile != null) {
                            List<ByteTransformer> execute = fileByteReaderResource.execute(
                                    new FileByteReaderRequest(readyFile, StatisticSec.class)
                            );
                            execute.forEach(byteItem -> broker.add((StatisticSec) byteItem, 6_000L));
                            try {
                                UtilFile.remove(readyFile);
                            } catch (Exception e) {
                                throw new ForwardException(e);
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
                            List<StatisticSec> reserveStatistic = promise.getRepositoryMap(StatisticUploaderPromiseProperty.RESERVE_STATISTIC.name(), List.class, null);
                            if (reserveStatistic != null && !reserveStatistic.isEmpty()) {
                                reserveStatistic.forEach(statisticSec -> broker.add(statisticSec, 2_000L));
                            }
                        }
                    }
                });
    }

}
