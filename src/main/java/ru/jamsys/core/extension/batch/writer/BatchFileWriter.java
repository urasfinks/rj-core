package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

// Теперь многопоточная запись в файл пачками по 4кб
// C - callback
@Getter
public class BatchFileWriter extends AbstractLifeCycle implements AutoCloseable, LifeCycleInterface {


    // Минимальный размер пачки в килобайтах, перед тем как данные будут записаны на файловую систему
    @Setter
    private volatile static int minBatchSize = (int) UtilByte.kilobytesToBytes(4); // 4KB

    private OutputStream fileOutputStream;

    // Конкурентная не блокирующая очередь, порядок добавления нам не критичен, главное, что бы не было блокировок
    private final ConcurrentLinkedDeque<byte[]> queue = new ConcurrentLinkedDeque<>();

    // Блокировка на запись, что бы только 1 поток мог писать данные на файловую систему
    private final AtomicBoolean flushLock = new AtomicBoolean(false);

    // Приблизительный размер наполнения пачки, состояние может быть не консистентно
    // Для нас этот показатель не критичен, так как есть фоновый процесс, который каждую секунду вызывает flush(false)
    private final AtomicLong writeBatchSize = new AtomicLong();

    // Точная позиция смещения данных относительно начала файла
    private final AtomicLong position = new AtomicLong(0);

    private final String filePath;

    @Setter
    private OpenOption openOption = StandardOpenOption.TRUNCATE_EXISTING;

    @SuppressWarnings("unused")
    public BatchFileWriter(String filePath) {
        this.filePath = filePath;
    }

    private void write(@NotNull byte[] data) throws Exception {
        if (!isRun()) {
            throw new IOException("Writer is closed");
        }
        queue.add(data);
        if (writeBatchSize.addAndGet(data.length) >= minBatchSize) {
            // Так как мы оперируем ресурсом потока, который просто хотел закинуть свою порцию данных
            // мы будем писать только одну пачку, а если там наваливают без остановки, нельзя так использовать поток
            // Там должен подключится фоновый процесс, если конечно у него получится втиснуться,
            // но как будто надо писать логику хитрую, что бы
            flush(true);
        }
    }

    private void flush(boolean onlyOneBatch) throws IOException {
        // Что бы только один поток мог писать на файловую систему
        if (flushLock.compareAndSet(false, true)) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                // Да, может случиться такое, что будет isEmpty и мы выйдем из цикла,
                // но реально другой поток положит в очередь новую порцию данных, а мы ещё не переключили lock в false
                // Тут должен подключится планировщик, который каждую секунду будет вызывать flush в режиме
                // onlyOneBatch = false, то есть без остановки будет писать на ФС столько пачек, сколько получится
                while (!queue.isEmpty()) {
                    byte[] data = queue.pollFirst();
                    if (data == null) {
                        continue;
                    }
                    byteArrayOutputStream.write(data);
                    if (byteArrayOutputStream.size() >= minBatchSize) {
                        fileOutputStream.write(byteArrayOutputStream.toByteArray());
                        byteArrayOutputStream.reset();
                        if (onlyOneBatch) {
                            break;
                        }
                    }
                }
                // Допустим сменился batchSize и мы не добрали на пачку, надо всё равно записать
                if (byteArrayOutputStream.size() > 0) {
                    fileOutputStream.write(byteArrayOutputStream.toByteArray());
                }
                writeBatchSize.set(0);
            } finally {
                flushLock.set(false);
            }
        }
    }

    @Override
    public void close() throws IOException {
        shutdown();
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
            flush(false);
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