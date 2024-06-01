package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.sub.ManagerElement;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LogWriter implements KeepAlive, LifeCycleInterface {

    @Getter
    ManagerElement<Broker<Log>, Void> broker;

    final String folder;

    @Setter
    int maxFileSizeByte;

    @Setter
    int maxFileCount;

    final String index;

    String currentFilePath;

    final AtomicInteger writeByteToCurrentFile = new AtomicInteger(0);

    final AtomicInteger counter = new AtomicInteger(0);

    public int size() {
        return broker.get().size();
    }

    public int getIndexFile() {
        return counter.get();
    }

    private void genNextFile() {
        if (currentFilePath != null) {
            UtilFile.rename(currentFilePath, currentFilePath.substring(0, currentFilePath.length() - 8) + "bin");
        }
        int curIndex = counter.getAndIncrement() % maxFileCount;
        currentFilePath = folder + "/" + index + "." + Util.padLeft(curIndex + "", (maxFileCount + "").length(), "0") + ".proc.bin";
    }

    public LogWriter(String index) {

        this.index = index;

        broker = App.context.getBean(BrokerManager.class).get(index, Log.class);
        // На практики не видел больше 400к логов на одном узле
        // Проверил запись 1кк логов - в секунду укладываемся на одном потоке
        broker.get().getRateLimit().get(RateLimitName.BROKER_SIZE.getName()).set(400_000);
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);

        folder = propertiesComponent.getProperties(index, "log.file.folder", String.class);
        maxFileSizeByte = propertiesComponent.getProperties(index, "log.file.size.mb", Integer.class) * 1024 * 1024;
        maxFileCount = propertiesComponent.getProperties(index, "log.file.count", Integer.class);

        restoreIndex();
    }

    private void restoreIndex() {
        List<String> filesRecursive = UtilFile.getFilesRecursive(folder, false);
        AvgMetric metric = new AvgMetric();
        for (String filePath : filesRecursive) {
            if (filePath.startsWith("/" + index + ".")) {
                if (filePath.endsWith(".proc.bin")) {
                    // Файл скорее всего имеет не корректную структуру, так как при нормально завершении
                    // файлы с расширение proc.bin должны были переименоваться
                    // Предполагается фатальное завершение прошлого процесса и такие файлы будут выкидываться
                    // (so sorry my bad) Дима Г. наблевавший в номере)
                    App.context.getBean(ExceptionHandler.class).handler(new RuntimeException("File will be remove: [" + filePath + "] so sorry my bad"));
                    try {
                        UtilFile.remove(folder + filePath);
                    } catch (Exception e) {
                        App.context.getBean(ExceptionHandler.class).handler(new RuntimeException("So sorry my bad twice"));
                    }
                } else {
                    // Что угодно может произойти, защищаемся от всего
                    try {
                        String substring = filePath.substring(index.length() + 2);
                        metric.add(Long.parseLong(substring.substring(0, substring.indexOf("."))));
                    } catch (Exception ignore) {
                    }
                }
            }
        }
        LongSummaryStatistics flush = metric.flush();
        if (flush.getCount() > 0) {
            counter.set((int) flush.getMax() + 1);
        }
    }

    public void append(Log log) {
        broker.get().add(log, 6_000);
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        if (currentFilePath == null) {
            genNextFile();
        }
        broker.accept(logBroker -> {
            int maxWriteCount = maxFileCount;
            while (!logBroker.isEmpty() && isThreadRun.get()) {
                if (maxWriteCount <= 0) {
                    break;
                }
                write(isThreadRun);
                maxWriteCount--;
            }
        });
    }

    private void write(AtomicBoolean isThreadRun) {
        try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(currentFilePath, writeByteToCurrentFile.get() > 0))) {
            broker.accept(logBroker -> {
                while (!logBroker.isEmpty() && isThreadRun.get()) {
                    try {
                        ExpirationMsImmutableEnvelope<Log> itemExpirationMsMutableEnvelope = logBroker.pollFirst();
                        if (itemExpirationMsMutableEnvelope != null) {
                            Log item = itemExpirationMsMutableEnvelope.getValue();
                            // Запись кол-ва заголовков
                            fos.write(UtilByte.shortToBytes((short) item.header.size()));
                            writeByteToCurrentFile.addAndGet(2);
                            // Запись заголовков
                            for (String key : item.header.keySet()) {
                                UtilLog.shortWriteString(fos, key, writeByteToCurrentFile);
                                UtilLog.shortWriteString(fos, item.header.get(key), writeByteToCurrentFile);
                            }
                            // Запись тела
                            UtilLog.writeString(fos, item.data, writeByteToCurrentFile);

                            if (writeByteToCurrentFile.get() > maxFileSizeByte) {
                                writeByteToCurrentFile.set(0);
                                genNextFile();
                                break;
                            }
                        }
                    } catch (Exception e) {
                        App.context.getBean(ExceptionHandler.class).handler(e);
                        break;
                    }
                }
            });
        } catch (Exception e) {
            // Предполагаю, что есть проблемы с файлом
            // Буду пробовать в другой записать
            genNextFile();
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    @Override
    public void run() {

    }

    @Override
    public void shutdown() {
        genNextFile();
    }
}
