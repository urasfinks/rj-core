package ru.jamsys.core.component.cron;

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
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.extension.ByteTransformer;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.extension.UniqueClassNameImpl;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
import ru.jamsys.core.flat.template.cron.release.Cron5s;
import ru.jamsys.core.flat.util.UtilListSort;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.jt.Logger;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.PoolSettingsRegistry;
import ru.jamsys.core.resource.filebyte.reader.FileByteReaderRequest;
import ru.jamsys.core.resource.filebyte.reader.FileByteReaderResource;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Component
@Lazy
public class LogUploader extends RepositoryPropertiesField implements Cron5s, PromiseGenerator, UniqueClassName {

    final Broker<Log> broker;

    @Setter
    @Getter
    private String index;

    private final ServicePromise servicePromise;

    @Getter
    @PropertyName("default.log.file.folder")
    private String folder;

    @PropertyName("run.args.remote.log.limit.points")
    private Integer limitInsert;

    @Getter
    @PropertyName("run.args.remote.log")
    private Boolean remoteLog;

    private final String idx;

    public enum LogUploaderPromiseProperty {
        RESERVE_LOG,
    }

    public LogUploader(ManagerBroker managerBroker, ApplicationContext applicationContext, ServicePromise servicePromise, ServiceProperty serviceProperty) {
        this.servicePromise = servicePromise;
        this.idx = UniqueClassNameImpl.getClassNameStatic(Log.class, null, applicationContext);
        broker = managerBroker.get(idx, Log.class);
        serviceProperty.getFactory().getPropertiesAgent(null, this, null, false);
    }

    @Override
    public Promise generate() {
        if (!remoteLog) {
            return null;
        }
        return servicePromise.get(index, 4_999L).appendWithResource("sendPostgreSQL", JdbcResource.class, "logger", (isThreadRun, task, promise, jdbcResource) -> {
            AtomicInteger countInsert = new AtomicInteger(0);

            JdbcRequest jdbcRequest = new JdbcRequest(Logger.INSERT);
            List<Log> reserve = new ArrayList<>();

            while (!broker.isEmpty() && isThreadRun.get() && countInsert.get() < limitInsert) {
                ExpirationMsImmutableEnvelope<Log> envelope = broker.pollFirst();
                if (envelope != null) {
                    Log log = envelope.getValue();
                    reserve.add(log);
                    jdbcRequest.addArg("date_add", log.getTimeAdd())
                            .addArg("type", log.getLogType().getNameCamel())
                            .addArg("correlation", log.getCorrelation())
                            .addArg("host", "localhost")
                            .addArg("ext_index", log.getExtIndex())
                            .addArg("data", log.getData())
                            .nextBatch();
                    countInsert.incrementAndGet();
                }
            }
            promise.setRepositoryMap(LogUploaderPromiseProperty.RESERVE_LOG.name(), reserve);
            if (countInsert.get() > 0) {
                try {
                    jdbcResource.execute(jdbcRequest);
                } catch (Throwable th) {
                    promise.setError(task, th);
                }
            }
        }).then("readDirectory", (_, _, promise) -> {
            List<String> filesRecursive = UtilFile.getFilesRecursive(getFolder(), false);
            List<String> restore = new ArrayList<>();
            for (String filePath : filesRecursive) {
                if (filePath.startsWith("/" + idx + ".") && !filePath.endsWith(".proc.bin")) {
                    restore.add(filePath);
                }
            }
            if (!restore.isEmpty()) {
                promise.setRepositoryMap("readyFile", getFolder() + UtilListSort.sortAsc(restore).getFirst());
            }
        }).appendWait().appendWithResource("read", FileByteReaderResource.class, (_, _, promise, fileByteReaderResource) -> {
            if (broker.getOccupancyPercentage() < 50) {
                String readyFile = promise.getRepositoryMap(String.class, "readyFile");
                if (readyFile != null) {
                    List<ByteTransformer> execute = fileByteReaderResource.execute(new FileByteReaderRequest(readyFile, Log.class));
                    execute.forEach(byteItem -> broker.add((Log) byteItem, 6_000L));
                    try {
                        UtilFile.remove(readyFile);
                    } catch (Exception e) {
                        throw new ForwardException(e);
                    }
                }
            }
        }).onError((_, _, promise) -> {
            Throwable exception = promise.getException();
            if (exception != null) {
                @SuppressWarnings("unchecked") Function<Throwable, Boolean> isFatalExceptionOnComplete = App
                        .get(PoolSettingsRegistry.class)
                        .get(JdbcResource.class, "logger")
                        .getFunctionCheckFatalException();

                if (isFatalExceptionOnComplete.apply(exception)) {
                    // Уменьшили срок с 6сек до 2сек, что бы при падении Influx быстрее сгрузить данные на файловую систему
                    List<Log> reserveLog = promise.getRepositoryMap(List.class, LogUploaderPromiseProperty.RESERVE_LOG.name());
                    if (reserveLog != null && !reserveLog.isEmpty()) {
                        reserveLog.forEach(log -> broker.add(log, 2_000L));
                    }
                }
            }
        });
    }

}
