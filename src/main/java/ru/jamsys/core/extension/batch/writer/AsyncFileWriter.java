package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.statistic.AvgMetric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

// public enum Operation {
//        SUBSCRIBE_GROUP((byte) 1), //Подписана группа
//        INSERT_DATA((byte) 2), // Добавлены данные
//        POLL_DATA_GROUP((byte) 3), // Данные были выданы для группы
//        COMMIT_DATA_GROUP((byte) 4), // Данные были обработаны группой
//        UNSUBSCRIBE_GROUP((byte) 5), // Группа отписана
//        ;

// Многопоточная запись в файл пачками
@Getter
public class AsyncFileWriter<T extends AbstractAsyncFileWriterElement>
        extends AbstractLifeCycle
        implements LifeCycleInterface, StatisticsFlush {

    private static final int defSize = (int) UtilByte.kilobytesToBytes(4); // 4KB

    // Минимальный размер пачки в килобайтах, перед тем как данные будут записаны на файловую систему
    @Setter
    private volatile static int minBatchSize = defSize;

    private OutputStream fileOutputStream;

    // Конкурентная не блокирующая очередь, порядок добавления нам не критичен, главное, что бы не было блокировок
    private final ConcurrentLinkedDeque<T> inputQueue = new ConcurrentLinkedDeque<>();

    // Блокировка на запись, что бы только 1 поток мог писать данные на файловую систему
    private final AtomicBoolean flushLock = new AtomicBoolean(false);

    // Точная позиция смещения данных относительно начала файла
    private final AtomicLong position = new AtomicLong(0);

    private final AvgMetric statisticSize = new AvgMetric();

    private final AvgMetric statisticTime = new AvgMetric();

    private final String filePath;

    @Setter
    private OpenOption openOption = StandardOpenOption.TRUNCATE_EXISTING;

    // Надо понимать, что onWrite будет запускаться планировщиком 1 раз в секунду и нельзя туда вешать долгие
    // IO операции. Перекладывайте ответы в свою локальную очередь и разбирайте их в других потоках
    private final Consumer<List<T>> onWrite;

    @SuppressWarnings("unused")
    public AsyncFileWriter(String filePath, Consumer<List<T>> onWrite) {
        this.filePath = filePath;
        this.onWrite = onWrite;
    }

    public void writeAsync(T data) throws Exception {
        if (!isRun()) {
            throw new IOException("Writer is closed");
        }
        inputQueue.add(data);
    }

    public void flush() throws IOException {
        // Что бы только один поток мог писать на файловую систему, планируется запись только в планировщике
        if (flushLock.compareAndSet(false, true)) {
            List<T> listPolled = new ArrayList<>();
            // Ограничиваем, что бы запись длилась не более 1 секунды
            long finishTime = System.currentTimeMillis() + 950;
            long lastTimeWrite = 0;
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while (!inputQueue.isEmpty()) {
                    long whiteStartTime = System.currentTimeMillis();
                    if (whiteStartTime > finishTime) {
                        break;
                    }
                    T polled = inputQueue.pollFirst();
                    if (polled == null) {
                        continue;
                    }
                    statisticSize.add((long) polled.getBytes().length);
                    polled.setPosition(position.getAndAdd(polled.getBytes().length));
                    listPolled.add(polled);
                    byteArrayOutputStream.write(polled.getBytes());
                    if (
                            byteArrayOutputStream.size() >= minBatchSize // Наполнили минимальную пачку
                                    || (whiteStartTime - lastTimeWrite) > 50 // Если с прошлой записи пачки прошло 50мс
                    ) {
                        lastTimeWrite = System.currentTimeMillis();
                        fileOutputStream.write(byteArrayOutputStream.toByteArray());
                        byteArrayOutputStream.reset();
                        if (onWrite != null) {
                            onWrite.accept(listPolled);
                        }
                        listPolled.clear();
                        statisticTime.add(System.currentTimeMillis() - lastTimeWrite);
                    }
                }
                // Если в буфере остались данные
                if (byteArrayOutputStream.size() > 0) {
                    long s = System.currentTimeMillis();
                    fileOutputStream.write(byteArrayOutputStream.toByteArray());
                    if (onWrite != null) {
                        onWrite.accept(listPolled);
                    }
                    statisticTime.add(System.currentTimeMillis() - s);
                }
            } finally {
                flushLock.set(false);
            }
        }
    }

    @Override
    public void runOperation() {
        try {
            this.fileOutputStream = Files.newOutputStream(
                    Paths.get(filePath),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    openOption,
                    // в таком режиме atime, mtime, размер файла может быть не синхронизован,
                    // но данные при восстановлении будут вычитаны корректно
                    StandardOpenOption.DSYNC
            );
        } catch (Throwable th) {
            throw new ForwardException(th);
        }
    }

    @Override
    public void shutdownOperation() {
        try {
            flush();
        } catch (Throwable th) {
            App.error(th);
        } finally {
            try {
                fileOutputStream.close();
            } catch (Throwable th) {
                App.error(th);
            }
        }
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        AvgMetric.Statistic sizeWrite = statisticSize.flushStatistic();
        // Получаем статистику, сколько всего было записано байт на прошлой итерации, для того, что бы сократить IOPS
        minBatchSize = (int) sizeWrite.getSum();
        if (minBatchSize < defSize) {
            minBatchSize = defSize;
        }
        float sByte = (float) minBatchSize;
        float sKb = sByte / 1024;
        float sMb = sKb / 1024;

        return List.of(new DataHeader()
                .addHeader("minBatchSizeByte", sByte)
                .addHeader("minBatchSizeKb", sKb)
                .addHeader("minBatchSizeMb", sMb)
                .addHeader("size", sizeWrite)
                .addHeader("time", statisticTime.flushStatistic())
        );
    }
}