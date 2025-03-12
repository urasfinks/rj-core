package ru.jamsys.core.component.cron;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.item.LogSimple;
import ru.jamsys.core.extension.ByteTransformer;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.flat.template.cron.release.Cron5s;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilListSort;
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
public class LogUploader extends AnnotationPropertyExtractor implements Cron5s, PromiseGenerator {

    final Broker<LogSimple> broker;

    private final ServicePromise servicePromise;

    @Getter
    @PropertyName("folder")
    private String folder = "LogManager";

    @PropertyName("limit.points")
    private Integer limitPoints = 2000;

    @Getter
    @PropertyName("remote")
    private Boolean remote = false;

    private final String idx;

    public enum LogUploaderPromiseProperty {
        RESERVE_LOG,
    }

    public LogUploader(
            ManagerBroker managerBroker,
            ServicePromise servicePromise,
            ApplicationContext applicationContext
    ) {
        this.servicePromise = servicePromise;
        this.idx = App.getUniqueClassName(LogSimple.class);
        broker = managerBroker.get(idx, LogSimple.class);
        new PropertyDispatcher(
                applicationContext.getBean(ServiceProperty.class),
                null,
                this,
                "log.uploader"
        ); //Без run() просто заполнить значения
    }

    @Override
    public Promise generate() {
        if (!remote) {
            return null;
        }
        return servicePromise
                .get(App.getUniqueClassName(getClass()), 4_999L)
                .appendWithResource("sendPostgreSQL", JdbcResource.class, "logger", (threadRun, task, promise, jdbcResource) -> {
                    AtomicInteger countInsert = new AtomicInteger(0);

                    JdbcRequest jdbcRequest = new JdbcRequest(Logger.INSERT);
                    List<LogSimple> reserve = new ArrayList<>();

                    while (!broker.isEmpty() && threadRun.get() && countInsert.get() < limitPoints) {
                        ExpirationMsImmutableEnvelope<LogSimple> envelope = broker.pollFirst();
                        if (envelope != null) {
                            LogSimple log = envelope.getValue();
                            reserve.add(log);
                            jdbcRequest.addArg("date_add", log.getTimeAdd())
                                    .addArg("type", log.getLogType().getNameCamel())
                                    //.addArg("correlation", log.getCorrelation())
                                    .addArg("host", "localhost")
                                    //.addArg("ext_index", log.getExtIndex())
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
                        promise.setRepositoryMap(
                                "readyFile",
                                getFolder() + UtilListSort.sort(restore, UtilListSort.Type.ASC).getFirst()
                        );
                    }
                }).appendWait().appendWithResource("read", FileByteReaderResource.class, (_, _, promise, fileByteReaderResource) -> {
                    if (broker.getOccupancyPercentage() < 50) {
                        String readyFile = promise.getRepositoryMap(String.class, "readyFile");
                        if (readyFile != null) {
                            List<ByteTransformer> execute = fileByteReaderResource.execute(new FileByteReaderRequest(readyFile, LogSimple.class));
                            execute.forEach(byteItem -> broker.add((LogSimple) byteItem, 6_000L));
                            try {
                                UtilFile.remove(readyFile);
                            } catch (Exception e) {
                                throw new ForwardException(e);
                            }
                        }
                    }
                }).onError((_, _, promise) -> {
                    Throwable exception = promise.getExceptionSource();
                    if (exception != null) {
                        @SuppressWarnings("unchecked") Function<Throwable, Boolean> isFatalExceptionOnComplete = App
                                .get(PoolSettingsRegistry.class)
                                .get(JdbcResource.class, "logger")
                                .getFunctionCheckFatalException();

                        if (isFatalExceptionOnComplete.apply(exception)) {
                            // Уменьшили срок с 6сек до 2сек, что бы при падении Influx быстрее сгрузить данные на файловую систему
                            List<LogSimple> reserveLog = promise.getRepositoryMap(List.class, LogUploaderPromiseProperty.RESERVE_LOG.name());
                            if (reserveLog != null && !reserveLog.isEmpty()) {
                                reserveLog.forEach(log -> broker.add(log, 2_000L));
                            }
                        }
                    }
                });
    }

}
