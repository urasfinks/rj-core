package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceLogger;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertiesAgent;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;
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
        PropertyUpdateDelegate,
        LifeCycleInterface {

    @Getter
    private Broker<ByteTransformer> broker;

    private String currentFilePath;

    final AtomicInteger writeByteToCurrentFile = new AtomicInteger(0);

    final AtomicInteger counter = new AtomicInteger(0);

    private final FileByteWriterProperties property = new FileByteWriterProperties();

    @Getter
    private final PropertiesAgent propertiesAgent;

    private final AtomicBoolean runWrite = new AtomicBoolean(false);

    public FileByteWriter(String ns) {
        ns = ns != null ? ns : "default";
        // На практики не видел больше 400к логов на одном узле
        // Проверил запись 1кк логов - в секунду укладываемся на одном потоке
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        propertiesAgent = serviceProperty.getFactory().getPropertiesAgent(this, property, ns, false);

        if (broker == null) {
            throw new RuntimeException("broker is null");
        }
        broker.getPropertyBrokerSize().set(400_000);

        if (property.getFileName() == null || property.getFileName().isEmpty()) {
            throw new RuntimeException("file name is empty");
        }
    }

    @Override
    public void onPropertyUpdate(Map<String, String> mapAlias) {
        if (mapAlias.containsKey("log.file.name")) {
            broker = App.get(ManagerBroker.class).initAndGet(
                    UniqueClassNameImpl.getClassNameStatic(FileByteWriter.class, property.getFileName(), App.context),
                    ByteTransformer.class,
                    null
            );
            restoreIndex();
        }
        if (mapAlias.containsKey("log.file.folder")) {
            if (!UtilFile.ifExist(property.getFolder())) {
                throw new RuntimeException("log.file.folder: " + property.getFolder() + "; not exist");
            }
        }
    }

    public int getIndexFile() {
        return counter.get();
    }

    private void restoreIndex() {
        List<String> filesRecursive = UtilFile.getFilesRecursive(property.getFolder(), false);
        AvgMetric metric = new AvgMetric();
        for (String filePath : filesRecursive) {
            if (filePath.startsWith("/" + property.getFileName() + ".")) {
                if (filePath.endsWith(".proc.bin")) {
                    // Файл скорее всего имеет не корректную структуру, так как при нормально завершении
                    // файлы с расширение proc.bin должны были переименоваться
                    // Предполагается фатальное завершение прошлого процесса и такие файлы будут выкидываться
                    // (so sorry my bad) Дима Г. наблевавший в номере)
                    App.get(ServiceLogger.class).add(
                            LogType.INFO,
                            new HashMapBuilder<String, Object>().append("exception", "File will be remove: [" + filePath + "] so sorry my bad"),
                            getClass().getSimpleName() + ".restoreIndex",
                            true
                    );
                    try {
                        UtilFile.remove(property.getFolder() + filePath);
                    } catch (Exception e) {
                        App.error(new RuntimeException("So sorry my bad twice"));
                    }
                } else {
                    // Что угодно может произойти, защищаемся от всего
                    try {
                        String substring = filePath.substring(property.getFileName().length() + 2);
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
        int curIndex = counter.getAndIncrement() % property.getFileCount();
        currentFilePath = property.getFolder()
                + "/" + property.getFileName()
                + "." + Util.padLeft(curIndex + "", String.valueOf(property.getFileCount()).length(), "0")
                + ".proc.bin";
    }

    public void append(ByteTransformer log) {
        setActivity();
        broker.add(log, 6_000);
    }

    @Override
    public void keepAlive(AtomicBoolean threadRun) {
        if (broker == null || broker.isEmpty()) {
            return;
        }
        if (currentFilePath == null) {
            genNextFile();
        }
        int maxWriteCount = property.getFileCount();
        while (!broker.isEmpty() && threadRun.get()) {
            if (maxWriteCount <= 0) {
                break;
            }
            write(threadRun);
            maxWriteCount--;
        }
    }

    private void write(AtomicBoolean threadRun) {
        // Что бы не допустить одновременного выполнения при остановки приложения, когда приходит ContextClosedEvent
        if (runWrite.compareAndSet(false, true)) {
            int tmpSizeKb = property.getFileSizeKb();
            try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(currentFilePath, writeByteToCurrentFile.get() > 0))) {
                while (!broker.isEmpty() && threadRun.get()) {
                    try {
                        ExpirationMsImmutableEnvelope<ByteTransformer> itemExpirationMsMutableEnvelope = broker.pollFirst();
                        if (itemExpirationMsMutableEnvelope != null) {
                            ByteTransformer item = itemExpirationMsMutableEnvelope.getValue();
                            byte[] d = item.getByteInstance();
                            fos.write(UtilByte.intToBytes(d.length));
                            writeByteToCurrentFile.addAndGet(4);
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
                // Предполагаю, что есть проблемы с файлом
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
    public void run() {
        propertiesAgent.run();
    }

    @Override
    public void shutdown() {
        // Запишем если были накопления в брокере
        keepAlive(new AtomicBoolean(true));
        // Переименуем файл, что бы при следующем старте его не удалили как ошибочный
        // Сначала не хотел закрывать файл, но решил, что надо, для того, что бы система могла уже с ним поработать
        // Если оставить его не закрытым, то он будет висеть до закрытия программы, что наверное не очень хорошо
        closeLastFile();
        propertiesAgent.shutdown();
    }

}
