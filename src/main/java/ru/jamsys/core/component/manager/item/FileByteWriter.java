package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.extension.broker.persist.BrokerMemory;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.flat.util.UtilText;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FileByteWriter extends ExpirationMsMutableImpl
        implements
        KeepAlive,
        StatisticsFlush,
        ClassEquals,
        PropertyListener,
        LifeCycleInterface {

    @Getter
    private final BrokerMemory<ByteSerialization> broker;

    private String currentFilePath;

    final AtomicInteger writeByteToCurrentFile = new AtomicInteger(0);

    final AtomicInteger counter = new AtomicInteger(0);

    private final FileByteProperty fileByteProperty = new FileByteProperty();

    @Getter
    private final PropertyDispatcher<Object> propertyDispatcher;

    private final AtomicBoolean runWrite = new AtomicBoolean(false);

    @Getter
    private final String key;

    public FileByteWriter(String key) {
        this.key = key;
        // На практики не видел больше 400к логов на одном узле.
        // Проверил запись 1кк логов - в секунду укладываемся на одном потоке
        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                this,
                fileByteProperty,
                key
        );
        restoreIndex(fileByteProperty.getFolder(), fileByteProperty.getFileName());
        // Это собственный брокер для текущего экземпляра, он копит то, что надо записывать на ФС
        broker = App.get(ManagerBroker.class).initAndGet(
                key,
                ByteSerialization.class,
                byteTransformer -> {
                    try {
                        UtilLog.error(FileByteWriter.class, new String(byteTransformer.toByte()))
                                .addHeader("exception", "drop broker key: " + key)
                                .print();
                    } catch (Exception e) {
                        App.error(e);
                    }
                } // Получается если получим перелимит или время протухло - теряем
        );

        App.get(ServiceProperty.class)
                .computeIfAbsent(
                        broker
                                .getPropertyDispatcher()
                                .getPropertyRepository()
                                .getByFieldNameConstants(BrokerProperty.Fields.size)
                                .getPropertyKey(),
                        null
                )
                .set(400_000);

        if (fileByteProperty.getFileName() == null || fileByteProperty.getFileName().isEmpty()) {
            throw new RuntimeException("file name is empty");
        }
    }

    public int getIndexFile() {
        return counter.get();
    }

    private void restoreIndex(String folder, String fileName) {
        List<String> filesRecursive = UtilFile.getFilesRecursive(folder, false);
        AvgMetric metric = new AvgMetric();
        for (String filePath : filesRecursive) {
            if (filePath.startsWith("/" + fileName + ".")) {
                if (filePath.endsWith(".proc.bin")) {
                    // Файл скорее всего имеет не корректную структуру, так как при нормально завершении.
                    // Файлы с расширением proc.bin должны были переименоваться.
                    // Предполагается фатальное завершение прошлого процесса и такие файлы будут выкидываться
                    // (so sorry my bad) Дима Г. наблевавший в номере
                    UtilLog
                            .error(getClass(), "File will be remove: [" + filePath + "] so sorry my bad")
                            .print()
                            .sendRemote();

                    try {
                        UtilFile.remove(folder + filePath);
                    } catch (Exception e) {
                        App.error(new RuntimeException("So sorry my bad twice"));
                    }
                } else {
                    // Что угодно может произойти, защищаемся от всего
                    try {
                        String substring = filePath.substring(fileName.length() + 2);
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

    private void closeLastFile() {
        if (currentFilePath != null) {
            UtilFile.rename(currentFilePath, currentFilePath.substring(0, currentFilePath.length() - 8) + "bin");
            currentFilePath = null;
        }
    }

    private void genNextFile() {
        closeLastFile();
        int curIndex = counter.getAndIncrement() % fileByteProperty.getFileCount();
        currentFilePath = fileByteProperty.getFolder()
                + "/" + fileByteProperty.getFileName()
                + "." + UtilText.padLeft(curIndex + "", String.valueOf(fileByteProperty.getFileCount()).length(), "0")
                + ".proc.bin";
    }

    public void append(ByteSerialization byteSerialization) {
        setActivity();
        broker.add(byteSerialization, 6_000);
    }

    @Override
    public void keepAlive(AtomicBoolean threadRun) {
        // Это не то место где надо писать на ФС, это должно быть в IO promise
        // keep alive синхронно вызывается для всех компонентов, мы будем тормозить этот процесс
    }

    public void flush(AtomicBoolean threadRun) {
        if (broker == null || broker.isEmpty()) {
            return;
        }
        if (currentFilePath == null) {
            genNextFile();
        }

        int maxWriteCount = fileByteProperty.getFileCount();
        UtilLog.printInfo(FileByteWriter.class, fileByteProperty);
        while (!broker.isEmpty() && threadRun.get()) {
            if (maxWriteCount <= 0) {
                break;
            }
            write(threadRun);
            maxWriteCount--;
        }
    }

    private void write(AtomicBoolean threadRun) {
        // Что бы не допустить одновременного выполнения при остановке приложения, когда приходит ContextClosedEvent
        if (runWrite.compareAndSet(false, true)) {
            int tmpSizeKb = fileByteProperty.getFileSizeKb();
            try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(currentFilePath, writeByteToCurrentFile.get() > 0))) {
                while (!broker.isEmpty() && threadRun.get()) {
                    try {
                        ExpirationMsImmutableEnvelope<ByteSerialization> itemExpirationMsMutableEnvelope = broker.pollFirst();
                        if (itemExpirationMsMutableEnvelope != null) {
                            ByteSerialization item = itemExpirationMsMutableEnvelope.getValue();
                            byte[] d = item.toByte();
                            fos.write(UtilByte.shortToBytes(item.getSubscriberStatusRead()));
                            fos.write(UtilByte.intToBytes(d.length));
                            writeByteToCurrentFile.addAndGet(6); //2 byte subscriberStatusRead + 4 byte item byte length
                            fos.write(d);
                            writeByteToCurrentFile.addAndGet(d.length);

                            if (writeByteToCurrentFile.get() > tmpSizeKb) {
                                writeByteToCurrentFile.set(0);
                                genNextFile();
                                break;
                            }
                        }
                    } catch (Exception e) {
                        App.error(e);
                        break;
                    }
                }
            } catch (Exception e) {
                // Предполагаю, что есть проблемы с файлом.
                // Буду пробовать в другой записать
                genNextFile();
                App.error(e);
            }
            runWrite.set(false);
        }
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean threadRun) {
        return List.of();
    }

    @Override
    public boolean classEquals(Class<?> classItem) {
        return true;
    }

    @Override
    public boolean isRun() {
        return propertyDispatcher.isRun();
    }

    @Override
    public void run() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdown() {
        // Запишем если были накопления в брокере
        keepAlive(new AtomicBoolean(true));
        // Переименуем файл, что бы при следующем старте его не удалили как ошибочный.
        // Сначала не хотел закрывать файл, но решил, что надо, для того, что бы система могла уже с ним поработать.
        // Если оставить его не закрытым, то он будет висеть до закрытия программы, что наверное не очень хорошо
        closeLastFile();
        propertyDispatcher.shutdown();
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (key.equals("file.name")) {
            // Сливаем данные с прошлым именем файлов
            restoreIndex(fileByteProperty.getFolder(), oldValue);
            // Сливаем данные с новым именем файлов
            restoreIndex(fileByteProperty.getFolder(), newValue);
        }
        if (key.equals("folder")) {
            if (!UtilFile.ifExist(fileByteProperty.getFolder())) {
                throw new RuntimeException("file.folder: " + fileByteProperty.getFolder() + "; not exist");
            }
            // Сливаем данные из прошлой директории
            restoreIndex(oldValue, fileByteProperty.getFileName());
            // Сливаем данные из новой директории
            restoreIndex(newValue, fileByteProperty.getFileName());
        }
    }

}
