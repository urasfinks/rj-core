package ru.jamsys.core.extension.batch.writer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ProcedureThrowing;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.statistic.AvgMetric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

// Многопоточная запись в файл пачками

@Getter
public class AbstractAsyncFileWriter<T extends AbstractAsyncFileWriterElement>
        extends AbstractLifeCycle
        implements
        LifeCycleInterface,
        StatisticsFlush,
        CascadeKey {

    @JsonIgnore
    public static Set<AbstractAsyncFileWriter<?>> set = Util.getConcurrentHashSet();

    @Setter
    public String filePath;

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

    // Надо понимать, что onWrite будет запускаться планировщиком 1 раз в секунду и нельзя туда вешать долгие
    // IO операции. Перекладывайте ответы в свою локальную очередь и разбирайте их в других потоках
    private final Consumer<List<T>> onWrite;

    private final AsyncFileWriterRepositoryProperty repositoryProperty = new AsyncFileWriterRepositoryProperty();

    private final PropertyDispatcher<Object> propertyDispatcher;

    private final StandardOpenOption standardOpenOption;

    @Setter
    @NonNull
    private ProcedureThrowing onOutOfPosition = () -> {
        throw new RuntimeException("Out of position");
    };

    @SuppressWarnings("unused")
    public AbstractAsyncFileWriter(
            ApplicationContext applicationContext,
            String ns,
            String filePath,
            Consumer<List<T>> onWrite,
            StandardOpenOption standardOpenOption
    ) {
        this.filePath = filePath;
        this.onWrite = onWrite;
        this.standardOpenOption = standardOpenOption;
        propertyDispatcher = new PropertyDispatcher<>(
                applicationContext.getBean(ServiceProperty.class),
                null,
                repositoryProperty,
                getCascadeKey(ns)
        );
    }

    public void writeAsync(T data) throws Throwable {
        if (!isRun()) {
            throw new IOException("Writer is closed");
        }
        if (position.get() > repositoryProperty.getMaxSize()) {
            onOutOfPosition.run();
        }
        inputQueue.add(data);
    }

    @SuppressWarnings("unused")
    public int getOccupancyPercentage() {
        // repositoryProperty.getMaxSize() - 100
        // position - x
        return (int) (((float) position.get()) * 100 / repositoryProperty.getMaxSize());
    }

    public void flush(AtomicBoolean threadRun) throws Throwable {
        // Что бы только один поток мог писать на файловую систему, планируется запись только в планировщике
        if (flushLock.compareAndSet(false, true)) {
            if (inputQueue.isEmpty()) {
                flushLock.set(false);
                return;
            }
            if (position.get() > repositoryProperty.getMaxSize()) {
                onOutOfPosition.run();
            }
            List<T> listPolled = new ArrayList<>();
            // Ограничиваем, что бы суммарная запись длилась не более 1с
            long finishTime = System.currentTimeMillis() + repositoryProperty.getFlushMaxTimeMs();
            int eachTime = repositoryProperty.getFlushEachTimeMs();
            long lastTimeWrite = 0;
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while (threadRun.get() && !inputQueue.isEmpty()) {
                    long whiteStartTime = System.currentTimeMillis();
                    if (whiteStartTime > finishTime) {
                        break;
                    }
                    T polled = inputQueue.pollFirst();
                    if (polled == null) {
                        continue;
                    }
                    int dataLength = polled.getBytes().length;
                    statisticSize.add((long) dataLength);
                    polled.setPosition(position.getAndAdd(polled.getBytes().length + 4)); // 4 это int length
                    listPolled.add(polled);
                    byteArrayOutputStream.write(UtilByte.intToBytes(dataLength));
                    byteArrayOutputStream.write(polled.getBytes());
                    if (
                            byteArrayOutputStream.size() >= minBatchSize // Наполнили минимальную пачку
                                    || (whiteStartTime - lastTimeWrite) > eachTime // Если с прошлой записи пачки прошло 50мс
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
            } catch (IOException ie) {
                // Вернём, что изъяли, но не смогли записать
                inputQueue.addAll(listPolled);
            } finally {
                flushLock.set(false);
            }
        }
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
        openOutputStream();
        set.add(this);
    }

    private void openOutputStream() {
        try {
            this.fileOutputStream = Files.newOutputStream(
                    Paths.get(getFilePath()),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    getStandardOpenOption(),
                    // в таком режиме atime, mtime, размер файла может быть не синхронизован,
                    // но данные при восстановлении будут вычитаны корректно
                    StandardOpenOption.DSYNC
            );
            position.set(0); // Начинаем с 0 позиции в новом вайле
        } catch (Throwable th) {
            throw new ForwardException(th);
        }
    }

    private void closeOutputStream() {
        try {
            fileOutputStream.close();
            // Если был открыт файл и в него записали ровно ничего - удалим его, зачем ему тут болтаться
            if (position.get() == 0) {
                Files.deleteIfExists(Paths.get(getFilePath()));
            }
        } catch (Throwable th) {
            App.error(th);
        }
    }

    // Может потребоваться если в runTime заменить filePath для rolling истории
    public void restartOutputStream() {
        closeOutputStream();
        openOutputStream();
    }

    @Override
    public void shutdownOperation() {
        set.remove(this);
        try {
            // getRun тут будет true, только после shutdownOperation станет false
            // Вроде как не очень логично, крутить сброс на ФС ожидая статус false, когда сами на него влияем
            // Однако у нас есть ограничения 1с, просто пытаемся сохранить что есть в очереди перед остановкой
            // ничего плохого не должно произойти за секунду
            flush(getRun());
        } catch (Throwable th) {
            App.error(th);
        } finally {
            closeOutputStream();
        }
        propertyDispatcher.shutdown();
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
                .addHeader("position", position.get())
                .addHeader("maxPosition", repositoryProperty.getMaxSize())
                .addHeader("minBatchSizeByte", sByte)
                .addHeader("minBatchSizeKb", sKb)
                .addHeader("minBatchSizeMb", sMb)
                .addHeader("size", sizeWrite)
                .addHeader("time", statisticTime.flushStatistic())
        );
    }

}