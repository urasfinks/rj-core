package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.sub.ManagerElement;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LogWriter implements KeepAlive {

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
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);

        folder = propertiesComponent.getProperties(index, "log.file.folder", String.class);
        maxFileSizeByte = propertiesComponent.getProperties(index, "log.file.size.mb", Integer.class) * 1024 * 1024;
        maxFileCount = propertiesComponent.getProperties(index, "log.file.count", Integer.class);

        genNextFile();
    }

    public void append(Log log) {
        broker.get().add(log, 6_000);
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
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

}
