package ru.jamsys.core.component.cron;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.extension.ByteItem;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.extension.property.PropertyConnector;
import ru.jamsys.core.extension.property.PropertyName;
import ru.jamsys.core.flat.template.cron.release.Cron5s;
import ru.jamsys.core.flat.util.ListSort;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.jt.Logger;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.PoolSettingsRegistry;
import ru.jamsys.core.resource.filebyte.reader.FileByteReaderRequest;
import ru.jamsys.core.resource.filebyte.reader.FileByteReaderResource;
import ru.jamsys.core.resource.influx.InfluxResource;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.statistic.StatisticSec;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class LogUploader extends PropertyConnector implements Cron5s, PromiseGenerator, ClassName {

    final Broker<Log> brokerLog;

    @Setter
    private String index;

    private final ServicePromise servicePromise;

    @Getter
    @PropertyName("default.log.file.folder")
    private String folder = "LogManager";

    @Getter
    @PropertyName("default.log.limit.insert")
    private String limitInsert = "10000";

    private String idx;

    public enum PromiseProperty {
        RESERVE_LOG,
    }

    public LogUploader(ManagerBroker managerBroker, ApplicationContext applicationContext, ServicePromise servicePromise, ServiceProperty serviceProperty) {
        this.servicePromise = servicePromise;
        this.idx = ClassNameImpl.getClassNameStatic(Log.class, null, applicationContext);
        brokerLog = managerBroker.get(idx, Log.class);
        serviceProperty.getSubscriber(null, this, getClassName(applicationContext), false);
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 2_000L).appendWithResource("sendPostgreSQL", JdbcResource.class, "logger", (isThreadRun, promise, influxResource) -> {
            int limitInsert = Integer.parseInt(this.limitInsert);
            AtomicInteger countInsert = new AtomicInteger(0);

            JdbcRequest jdbcRequest = new JdbcRequest(Logger.INSERT);
            List<Log> reserve = new ArrayList<>();

            while (!brokerLog.isEmpty() && isThreadRun.get() && countInsert.get() < limitInsert) {
                ExpirationMsImmutableEnvelope<Log> envelope = brokerLog.pollFirst();
                if (envelope != null) {
                    Log log = envelope.getValue();
                    reserve.add(log);
                    jdbcRequest.addArg("date_add", log.getTimeAdd())
                            .addArg("type", log.getLogType().getName())
                            .addArg("correlation", log.getCorrelation())
                            .addArg("host", "localhost")
                            .addArg("ext_index", log.getExtIndex())
                            .addArg("data", log.getData())
                            .nextBatch();
                    countInsert.incrementAndGet();
                }
            }
            promise.setProperty(PromiseProperty.RESERVE_LOG.name(), reserve);
            try {
                influxResource.execute(jdbcRequest);
            } catch (Throwable th) {
                promise.setErrorInRunTask(th);
            }
        }).then("readDirectory", (_, promise) -> {
            List<String> filesRecursive = UtilFile.getFilesRecursive(getFolder(), false);
            List<String> restore = new ArrayList<>();
            for (String filePath : filesRecursive) {
                if (filePath.startsWith("/" + idx + ".") && !filePath.endsWith(".proc.bin")) {
                    restore.add(filePath);
                }
            }
            if (!restore.isEmpty()) {
                promise.setProperty("readyFile", getFolder() + ListSort.sort(restore).getFirst());
            }
        }).appendWait().appendWithResource("read", FileByteReaderResource.class, (_, promise, fileByteReaderResource) -> {
            if (brokerLog.getOccupancyPercentage() < 50) {
                String readyFile = promise.getProperty("readyFile", String.class);
                if (readyFile != null) {
                    List<ByteItem> execute = fileByteReaderResource.execute(new FileByteReaderRequest(readyFile, Log.class));
                    execute.forEach(byteItem -> brokerLog.add((Log) byteItem, 6_000L));
                    try {
                        UtilFile.remove(readyFile);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).onError((_, promise) -> {
            Throwable exception = promise.getException();
            if (exception != null) {
                @SuppressWarnings("unchecked") Function<Throwable, Boolean> isFatalExceptionOnComplete = App
                        .get(PoolSettingsRegistry.class)
                        .get(JdbcResource.class, "logger")
                        .getIsFatalExceptionOnComplete();

                if (isFatalExceptionOnComplete.apply(exception)) {
                    // Уменьшили срок с 6сек до 2сек, что бы при падении Influx быстрее сгрузить данные на файловую систему
                    List<StatisticSec> reserveLog = promise.getProperty(PromiseProperty.RESERVE_LOG.name(), List.class, null);
                    if (reserveLog != null && !reserveLog.isEmpty()) {
                        reserveLog.forEach(statisticSec -> brokerLog.add(statisticSec, 2_000L));
                    }
                }
            }
        });
    }

}