package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.extension.ByteItem;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FileByteWriter implements KeepAlive, LifeCycleInterface {

    @Getter
    Broker<ByteItem> broker;

    String folder;

    @Setter
    int maxFileSizeByte;

    @Setter
    int maxFileCount;

    final String index;

    String currentFilePath;

    final AtomicInteger writeByteToCurrentFile = new AtomicInteger(0);

    final AtomicInteger counter = new AtomicInteger(0);

    public FileByteWriter(String index) {

        this.index = index;

        broker = App.context.getBean(BrokerManager.class).initAndGet(index, ByteItem.class, null);
        // На практики не видел больше 400к логов на одном узле
        // Проверил запись 1кк логов - в секунду укладываемся на одном потоке
        broker.getRateLimit().get(RateLimitName.BROKER_SIZE.getName()).set(400_000);
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);

        propertiesComponent.getProperties(index, "log.file.folder", String.class, s -> folder = s);
        propertiesComponent.getProperties(index, "log.file.size.mb", Integer.class, integer -> maxFileSizeByte = integer * 1024 * 1024);
        propertiesComponent.getProperties(index, "log.file.count", Integer.class, integer -> maxFileCount = integer);

        restoreIndex();
    }

    public int size() {
        return broker.size();
    }

    public int getIndexFile() {
        return counter.get();
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

    private void genNextFile() {
        if (currentFilePath != null) {
            UtilFile.rename(currentFilePath, currentFilePath.substring(0, currentFilePath.length() - 8) + "bin");
        }
        int curIndex = counter.getAndIncrement() % maxFileCount;
        currentFilePath = folder + "/" + index + "." + Util.padLeft(curIndex + "", (maxFileCount + "").length(), "0") + ".proc.bin";
    }

    public void append(ByteItem log) {
        broker.add(log, 6_000);
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        if (currentFilePath == null) {
            genNextFile();
        }
        int maxWriteCount = maxFileCount;
        while (!broker.isEmpty() && isThreadRun.get()) {
            if (maxWriteCount <= 0) {
                break;
            }
            write(isThreadRun);
            maxWriteCount--;
        }
    }

    private void write(AtomicBoolean isThreadRun) {
        try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(currentFilePath, writeByteToCurrentFile.get() > 0))) {
            while (!broker.isEmpty() && isThreadRun.get()) {
                try {
                    ExpirationMsImmutableEnvelope<ByteItem> itemExpirationMsMutableEnvelope = broker.pollFirst();
                    if (itemExpirationMsMutableEnvelope != null) {
                        ByteItem item = itemExpirationMsMutableEnvelope.getValue();
                        byte[] d = item.getByteInstance();
                        fos.write(UtilByte.intToBytes(d.length));
                        writeByteToCurrentFile.addAndGet(4);
                        fos.write(d);
                        writeByteToCurrentFile.addAndGet(d.length);

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