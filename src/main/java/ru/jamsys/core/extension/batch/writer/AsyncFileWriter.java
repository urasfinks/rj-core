package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.UtilByte;

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

// public enum Operation {
//        SUBSCRIBE_GROUP((byte) 1), //Подписана группа
//        INSERT_DATA((byte) 2), // Добавлены данные
//        POLL_DATA_GROUP((byte) 3), // Данные были выданы для группы
//        COMMIT_DATA_GROUP((byte) 4), // Данные были обработаны группой
//        UNSUBSCRIBE_GROUP((byte) 5), // Группа отписана
//        ;

// Многопоточная запись в файл пачками
@Getter
public class AsyncFileWriter<T extends AbstractAsyncFileWriterElement> extends AbstractLifeCycle implements LifeCycleInterface {

    // Минимальный размер пачки в килобайтах, перед тем как данные будут записаны на файловую систему
    @Setter
    private volatile static int minBatchSize = (int) UtilByte.kilobytesToBytes(4); // 4KB

    private OutputStream fileOutputStream;

    // Конкурентная не блокирующая очередь, порядок добавления нам не критичен, главное, что бы не было блокировок
    private final ConcurrentLinkedDeque<T> inputQueue = new ConcurrentLinkedDeque<>();

    // В выходную очередь будут записываться элементы записанные на FS
    private final ConcurrentLinkedDeque<T> outputQueue = new ConcurrentLinkedDeque<>();

    // Блокировка на запись, что бы только 1 поток мог писать данные на файловую систему
    private final AtomicBoolean flushLock = new AtomicBoolean(false);

    // Приблизительный размер наполнения пачки, состояние может быть не консистентно
    // Для нас этот показатель не критичен, так как есть фоновый процесс, который каждую секунду вызывает flush()
    private final AtomicLong writeBatchSize = new AtomicLong();

    // Точная позиция смещения данных относительно начала файла
    private final AtomicLong position = new AtomicLong(0);

    private final String filePath;

    @Setter
    private OpenOption openOption = StandardOpenOption.TRUNCATE_EXISTING;

    @SuppressWarnings("unused")
    public AsyncFileWriter(String filePath) {
        this.filePath = filePath;
    }

    public void writeAsync(T data) throws Exception {
        if (!isRun()) {
            throw new IOException("Writer is closed");
        }
        inputQueue.add(data);
    }

    private void flush() throws IOException {
        // Что бы только один поток мог писать на файловую систему, планируется запись только в планировщике
        if (flushLock.compareAndSet(false, true)) {
            List<T> listPolled = new ArrayList<>();
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while (!inputQueue.isEmpty()) {
                    T polled = inputQueue.pollFirst();
                    if (polled == null) {
                        continue;
                    }
                    polled.setPosition(position.getAndAdd(polled.getBytes().length));
                    listPolled.add(polled);
                    byteArrayOutputStream.write(polled.getBytes());
                    if (byteArrayOutputStream.size() >= minBatchSize) {
                        fileOutputStream.write(byteArrayOutputStream.toByteArray());
                        byteArrayOutputStream.reset();
                        outputQueue.addAll(listPolled);
                        listPolled.clear();
                    }
                }
                // Если в буфере остались данные
                if (byteArrayOutputStream.size() > 0) {
                    fileOutputStream.write(byteArrayOutputStream.toByteArray());
                    outputQueue.addAll(listPolled);
                }
                writeBatchSize.set(0);
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

}