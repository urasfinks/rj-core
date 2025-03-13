package ru.jamsys.core.component.cron;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.Core;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.ByteTransformer;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.flat.template.cron.release.Cron5s;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilListSort;
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

@SuppressWarnings("unused")
@Component
@Lazy
public class UploaderStatistic extends AnnotationPropertyExtractor implements Cron5s, PromiseGenerator, CascadeName {

    final Broker<StatisticSec> broker;

    private final ServicePromise servicePromise;

    @SuppressWarnings("all")
    @Getter
    @PropertyName("folder")
    private String folder = "LogManager";

    @SuppressWarnings("all")
    @Getter
    @PropertyName("limit.points")
    private Integer limitPoints = 5000;

    @SuppressWarnings("all")
    @Getter
    @PropertyName("remote")
    private Boolean remote = false;

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeName getParentCascadeName() {
        return App.cascadeName;
    }

    public enum StatisticUploaderPromiseProperty {
        RESERVE_STATISTIC,
    }

    public UploaderStatistic(
            ManagerBroker managerBroker,
            ServicePromise servicePromise,
            ServiceProperty serviceProperty
    ) {
        this.servicePromise = servicePromise;
        broker = App.get(Core.class).getStatisticSecBroker();
        new PropertyDispatcher(
                serviceProperty,
                null,
                this,
                "statistic.uploader"
        ); //Без run() просто заполнить значения
    }

    @Override
    public Promise generate() {
        if (!remote) {
            return null;
        }
        return servicePromise.get(App.getUniqueClassName(getClass()), 4_999L)
                .append("checkStatistic", (_, _, promise) -> {
                    if (broker.isEmpty()) {
                        promise.skipAllStep("broker.isEmpty()");
                    }
                })
                .thenWithResource("sendToInflux", InfluxResource.class, (threadRun, _, promise, influxResource) -> {
                    AtomicInteger countInsert = new AtomicInteger(0);
                    List<Point> listPoints = new ArrayList<>();

                    List<StatisticSec> reserve = new ArrayList<>();
                    while (!broker.isEmpty() && threadRun.get() && countInsert.get() < limitPoints) {
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
                    promise.setRepositoryMap(StatisticUploaderPromiseProperty.RESERVE_STATISTIC.name(), reserve);

                    if (countInsert.get() > 0) {
                        influxResource.execute(listPoints);
                    }
                })
                .then("readDirectory", (_, _, promise) -> {
                    String indexStatistic = getCascadeName();
                    List<String> filesRecursive = UtilFile.getFilesRecursive(getFolder(), false);
                    List<String> restore = new ArrayList<>();
                    for (String filePath : filesRecursive) {
                        if (filePath.startsWith("/" + indexStatistic + ".") && !filePath.endsWith(".proc.bin")) {
                            restore.add(filePath);
                        }
                    }
                    if (!restore.isEmpty()) {
                        promise.setRepositoryMap(
                                "readyFile",
                                getFolder() + UtilListSort.sort(restore, UtilListSort.Type.ASC).getFirst()
                        );
                    }
                })
                .appendWait()
                .appendWithResource("read", FileByteReaderResource.class, (_, _, promise, fileByteReaderResource) -> {
                    if (broker.getOccupancyPercentage() < 50) {
                        String readyFile = promise.getRepositoryMap(String.class, "readyFile");
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
                .onError((_, _, promise) -> {
                    Throwable exception = promise.getExceptionSource();
                    if (exception != null) {
                        @SuppressWarnings("unchecked")
                        Function<Throwable, Boolean> isFatalExceptionOnComplete = App
                                .get(PoolSettingsRegistry.class)
                                .get(InfluxResource.class, "default")
                                .getFunctionCheckFatalException();
                        if (isFatalExceptionOnComplete.apply(exception)) {
                            // Уменьшили срок с 6сек до 2сек, что бы при падении Influx быстрее сгрузить данные на файловую систему
                            List<StatisticSec> reserveStatistic = promise.getRepositoryMap(
                                    List.class,
                                    StatisticUploaderPromiseProperty.RESERVE_STATISTIC.name()
                            );
                            if (reserveStatistic != null && !reserveStatistic.isEmpty()) {
                                reserveStatistic.forEach(statisticSec -> broker.add(statisticSec, 2_000L));
                            }
                        }
                    }
                });
    }

}
