package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceLogger;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.extension.property.Subscriber;
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
        Closable,
        StatisticsFlush,
        CheckClassItem,
        LifeCycleInterface {

    @Getter
    Broker<ByteItem> broker;

    final String index;

    String currentFilePath;

    final AtomicInteger writeByteToCurrentFile = new AtomicInteger(0);

    final AtomicInteger counter = new AtomicInteger(0);

    private final FileByteWriterProperty property = new FileByteWriterProperty();

    private final Subscriber subscriber;

    private final AtomicBoolean isRunWrite = new AtomicBoolean(false);

    public FileByteWriter(String index, ApplicationContext applicationContext) {

        this.index = index;

        broker = App.get(ManagerBroker.class).initAndGet(
                ClassNameImpl.getClassNameStatic(FileByteWriter.class, index, applicationContext),
                ByteItem.class,
                null
        );
        // На практики не видел больше 400к логов на одном узле
        // Проверил запись 1кк логов - в секунду укладываемся на одном потоке
        broker.setMaxSizeQueue(400_000);
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        subscriber = serviceProperty.getSubscriber(null, property, index, false);

        restoreIndex();
    }

    public void setProperty(String prop, String value) {
        subscriber.setProperty(prop, value);
    }

    public int size() {
        return broker.size();
    }

    public int getIndexFile() {
        return counter.get();
    }

    private void restoreIndex() {
        List<String> filesRecursive = UtilFile.getFilesRecursive(property.getFolder(), false);
        AvgMetric metric = new AvgMetric();
        for (String filePath : filesRecursive) {
            if (filePath.startsWith("/" + index + ".")) {
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

    private void closeLastFile() {
        if (currentFilePath != null) {
            UtilFile.rename(currentFilePath, currentFilePath.substring(0, currentFilePath.length() - 8) + "bin");
            currentFilePath = null;
        }
    }

    private void genNextFile() {
        closeLastFile();
        int curIndex = counter.getAndIncrement() % Integer.parseInt(property.getFileCount());
        currentFilePath = property.getFolder() + "/" + index + "." + Util.padLeft(curIndex + "", property.getFileCount().length(), "0") + ".proc.bin";
    }

    public void append(ByteItem log) {
        active();
        broker.add(log, 6_000);
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        if (broker.isEmpty()) {
            return;
        }
        if (currentFilePath == null) {
            genNextFile();
        }
        int maxWriteCount = Integer.parseInt(property.getFileCount());
        while (!broker.isEmpty() && isThreadRun.get()) {
            if (maxWriteCount <= 0) {
                break;
            }
            write(isThreadRun);
            maxWriteCount--;
        }
    }

    private void write(AtomicBoolean isThreadRun) {
        // Что бы не допустить одновременного выполнения при остановки приложения, когда приходит ContextClosedEvent
        if (isRunWrite.compareAndSet(false, true)) {
            int tmpSizeKb = Integer.parseInt(property.getFileSizeKb());
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
            isRunWrite.set(false);
        }
    }

    @Override
    public void close() {
        // Запишем что накопили
        keepAlive(new AtomicBoolean(true));
        // Сначала не хотел закрывать файл, но решил, что надо, для того, что бы система могла уже с ним поработать
        // Если оставить его не закрытым, то он будет висеть до закрытия программы, что наверное не очень хорошо
        closeLastFile();
        // Из менаджера ссылки не исчезают, можно по идеи и не отписываться (Перетекание из map -> mapReserved)
        subscriber.unsubscribe();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        return List.of();
    }

    @Override
    public boolean checkClassItem(Class<?> classItem) {
        return true;
    }

    @Override
    public void run() {
        subscriber.init(false);
    }

    @Override
    public void shutdown() {
        // Запишем если были накопления в брокере
        keepAlive(new AtomicBoolean(true));
        // Переименуем файл, что бы при следующем старте его не удалили как ошибочный
        closeLastFile();
    }

}
