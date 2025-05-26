package ru.jamsys.core.extension.async.writer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.extension.broker.persist.BrokerPersistRepositoryProperty;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ProcedureThrowing;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

// Многопоточная запись в файл пачками

@Getter
public class AbstractAsyncFileWriter<T extends Position & ByteSerializable>
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        ManagerElement,
        CascadeKey {

    @Setter
    public String filePath;

    private static final int defSize = (int) UtilByte.kilobytesToBytes(4); // 4KB

    // Минимальный размер пачки в килобайтах, перед тем как данные будут записаны на файловую систему
    @Setter
    private volatile static int minBatchSize = defSize;

    @JsonIgnore
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
    private BiConsumer<String, List<T>> onWrite; // T - filePath; U - list written object

    private BrokerPersistRepositoryProperty repositoryProperty;

    private StandardOpenOption standardOpenOption;

    @Setter
    @NonNull
    private ProcedureThrowing onOutOfPosition = () -> {
        throw new RuntimeException("Out of position");
    };

    @SuppressWarnings("unused")
    public AbstractAsyncFileWriter(String filePath) {
        this.filePath = filePath;
    }

    public void setupRepositoryProperty(BrokerPersistRepositoryProperty repositoryProperty) {
        this.repositoryProperty = repositoryProperty;
    }

    public void setupStandardOpenOption(StandardOpenOption standardOpenOption) {
        this.standardOpenOption = standardOpenOption;
    }

    public void setupOnWrite(BiConsumer<String, List<T>> onWrite) {
        this.onWrite = onWrite;
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("filePath", filePath)
                ;
    }

    public void writeAsync(T data) {
        markActive();
        if (!isRun()) {
            throw new RuntimeException("Writer is closed");
        }
        if (position.get() > repositoryProperty.getMaxSize()) {
            try {
                onOutOfPosition.run();
            } catch (Throwable th) {
                throw new RuntimeException(th);
            }
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
                    long whileStartTime = System.currentTimeMillis();
                    if (whileStartTime > finishTime) {
                        break;
                    }
                    T poll = inputQueue.pollFirst();
                    if (poll == null) {
                        break;
                    }
                    int dataLength = poll.toBytes().length;
                    statisticSize.add((long) dataLength);
                    poll.setPosition(position.getAndAdd(poll.toBytes().length + 4)); // 4 это int length
                    listPolled.add(poll);
                    byteArrayOutputStream.write(UtilByte.intToBytes(dataLength));
                    byteArrayOutputStream.write(poll.toBytes());
                    if (
                            position.get() > repositoryProperty.getMaxSize()
                                    || byteArrayOutputStream.size() >= minBatchSize // Наполнили минимальную пачку
                                    || (whileStartTime - lastTimeWrite) > eachTime // Если с прошлой записи пачки прошло 50мс
                    ) {
                        lastTimeWrite = System.currentTimeMillis();
                        // Должны пометить, что мы активны, так как может быть записи уже и нет, но мы ещё не успели
                        // записать всё на файловую систему
                        markActive();
                        fileOutputStream.write(byteArrayOutputStream.toByteArray());
                        byteArrayOutputStream.reset();
                        if (onWrite != null) {
                            onWrite.accept(getFilePath(), listPolled);
                        }
                        listPolled.clear();
                        statisticTime.add(System.currentTimeMillis() - lastTimeWrite);

                        if (position.get() > repositoryProperty.getMaxSize()) {
                            onOutOfPosition.run();
                        }

                    }
                }
                // Если в буфере остались данные
                if (byteArrayOutputStream.size() > 0) {
                    long s = System.currentTimeMillis();
                    // Должны пометить, что мы активны, так как может быть записи уже и нет, но мы ещё не успели
                    // записать всё на файловую систему
                    markActive();
                    fileOutputStream.write(byteArrayOutputStream.toByteArray());
                    if (onWrite != null) {
                        onWrite.accept(getFilePath(), listPolled);
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
        if (standardOpenOption == null) {
            throw new RuntimeException("standardOpenOption is null filePath: " + filePath);
        }
        if (repositoryProperty == null) {
            throw new RuntimeException("repositoryProperty is null filePath: " + filePath);
        }
        openOutputStream();
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
            position.set(0); // Начинаем с 0 позиции в новом файле
        } catch (Throwable th) {
            throw new ForwardException(th);
        }
    }

    private void closeOutputStream() {
        // Перед закрытием запишем, что размер следующего блока будет -1, это будет меткой, что в файл больше ничего не
        // будет записано
        try {
            fileOutputStream.write(UtilByte.intToBytes(-1));
        } catch (Throwable th) {
            App.error(th);
        }
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
    // Контролируйте, что бы этот метод вызывался из onOutOfPosition в .flush(), что бы не нарушить консистентность
    // записи в файл, другими словами если в соседнем потоке вызвать перезапуск и в этот момент будет выполняться
    // .flush(), который пишет в fileOutputStream и мы его остановим - это очень плохо
    public void restartOutputStream() {
        if (isRun()) {
            closeOutputStream();
            openOutputStream();
        }
    }

    @Override
    public void shutdownOperation() {
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