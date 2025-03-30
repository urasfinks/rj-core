package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.flat.util.UtilByte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

// Теперь многопоточная запись в файл пачками по 4кб
// C - callback
@Getter
public class BatchFileWriter<C extends FileDataPosition> implements AutoCloseable {

    @Getter
    @Setter
    private static class ComplexData<C> {
        private final byte[] data;
        private final C callback;

        public ComplexData(byte[] data, C callback) {
            this.data = data;
            this.callback = callback;
        }
    }

    // Минимальный размер пачки в килобайтах, перед тем как данные будут записаны на файловую систему
    @Setter
    private volatile static int minBatchSize = (int) UtilByte.kilobytesToBytes(4); // 4KB

    private final OutputStream fileOutputStream;

    // Состояние закрытия файла, что бы не получилось сделать двойное закрытие и вставлять данные если файл закрыт
    private final AtomicBoolean closed = new AtomicBoolean(false);

    // Конкурентная не блокирующая очередь, порядок добавления нам не критичен, главное, что бы не было блокировок
    private final ConcurrentLinkedDeque<ComplexData<C>> queue = new ConcurrentLinkedDeque<>();

    // Блокировка на запись, что бы только 1 поток мог писать данные на файловую систему
    private final ReentrantLock flushLock = new ReentrantLock(true); // fair lock

    // Приблизительный размер наполнения пачки, состояние может быть не консистентно
    // Для нас этот показатель не критичен, так как есть фоновый процесс, который каждую секунду вызывает flush(false)
    private final AtomicLong writeBatchSize = new AtomicLong();

    // Точная позиция смещения данных относительно начала файла
    private final AtomicLong position = new AtomicLong(0);

    // Consumer в который передаются callback объекты записанные на файловую систему
    @Setter
    private Consumer<List<C>> onFlush;

    @SuppressWarnings("unused")
    public BatchFileWriter(String filePath) throws IOException {
        this(Paths.get(filePath));
    }

    public BatchFileWriter(Path filePath) throws IOException {
        this.fileOutputStream = Files.newOutputStream(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                // в таком режиме atime, mtime, размер файла может быть не синхронизован,
                // но данные при восстановлении будут вычитаны корректно
                StandardOpenOption.DSYNC
        );
    }

    private void write(@NotNull ComplexData<C> data) throws Exception {
        if (closed.get()) {
            throw new IOException("Writer is closed");
        }
        queue.add(data);
        if (writeBatchSize.addAndGet(data.getData().length) >= minBatchSize) {
            // Так как мы оперируем ресурсом потока, который просто хотел закинуть свою порцию данных
            // мы будем писать только одну пачку, а если там наваливают без остановки, нельзя так использовать поток
            // Там должен подключится фоновый процесс, если конечно у него получится втиснуться,
            // но как будто надо писать логику хитрую, что бы
            flush(true);
        }
    }

    public void write(@NotNull byte[] data, C callback) throws Exception {
        write(new ComplexData<>(data, callback));
    }

    public void write(@NotNull byte[] data) throws Exception {
        write(new ComplexData<>(data, null));
    }

    private void flush(boolean onlyOneBatch) throws IOException {
        // Что бы только один поток мог писать на файловую систему
        if (!flushLock.tryLock()) {
            return;
        }
        try {
            List<C> result = new ArrayList<>();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Да, может случиться такое, что будет isEmpty и мы выйдем из цикла,
            // но реально другой поток положит в очередь новую порцию данных, а мы ещё не переключили lock в false
            // Тут должен подключится планировщик, который каждую секунду будет вызывать flush в режиме
            // onlyOneBatch = false, то есть без остановки будет писать на ФС столько пачек, сколько получится
            while (!queue.isEmpty()) {
                ComplexData<C> complexData = queue.pollFirst();
                if (complexData == null) {
                    continue;
                }
                C callback = complexData.getCallback();
                // Вся эта история сделана только для того, что бы отслеживать запись на диск,
                // для того, что бы можно было запускать дальнейшую обработку этой информации.
                // Поэтому проверять наличие onFlush != null тут не будем.
                // Отсутствие callback и так говорит о том, что нет onFlush
                if (callback != null) {
                    int newLength = complexData.getData().length;
                    callback
                            .setFileDataPosition(position.getAndAdd(newLength))
                            .setFileDataLength(newLength);
                    result.add(callback);
                }
                byteArrayOutputStream.write(complexData.getData());
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
            if (onFlush != null && !result.isEmpty()) {
                onFlush.accept(result);
            }
            writeBatchSize.set(0);
        } finally {
            flushLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            try {
                flush(false);
            } finally {
                fileOutputStream.close();
            }
        }
    }

}