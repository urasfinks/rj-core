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
import java.util.function.Consumer;

// Теперь многопоточная запись в файл пачками по 4кб
// C - callback
@Getter
public class BatchFileWriter<C> implements AutoCloseable {

    @Getter
    @Setter
    public static class ComplexData<C> {
        private final byte[] data;
        private final C callback;

        public ComplexData(byte[] data, C callback) {
            this.data = data;
            this.callback = callback;
        }
    }

    @Setter
    private static int batchSize = (int) UtilByte.kilobytesToBytes(4); // 4KB

    private final OutputStream outputStream;

    private boolean closed;

    private final ConcurrentLinkedDeque<ComplexData<C>> queue = new ConcurrentLinkedDeque<>();

    private final AtomicBoolean lock = new AtomicBoolean(false);

    private final AtomicLong size = new AtomicLong();

    @Setter
    private Consumer<List<C>> onFlush;

    @SuppressWarnings("unused")
    public BatchFileWriter(String filePath) throws IOException {
        this(Paths.get(filePath));
    }

    public BatchFileWriter(Path filePath) throws IOException {
        this.outputStream = Files.newOutputStream(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
                // в таком режиме atime, mtime, размер файла может быть не синхронизован,
                // но данные при восстановлении будут вычитаны корректно
                StandardOpenOption.DSYNC
        );
        this.closed = false;
    }

    public void write(@NotNull ComplexData<C> data) throws Exception {
        if (closed) {
            throw new IOException("Writer is closed");
        }
        queue.add(data);
        if (size.addAndGet(data.getData().length) >= batchSize) {
            flush();
        }
    }

    public void write(@NotNull byte[] data, C callback) throws Exception {
        write(new ComplexData<>(data, callback));
    }

    public void write(@NotNull byte[] data) throws Exception {
        write(new ComplexData<>(data, null));
    }

    private void flush() throws IOException {
        // Что бы только кто-то один мог писать
        if (lock.compareAndSet(false, true)) {
            List<C> result = new ArrayList<>();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while (!queue.isEmpty()) {
                ComplexData<C> complexData = queue.pollFirst();
                if (complexData == null) {
                    continue;
                }
                C callback = complexData.getCallback();
                if (callback != null) {
                    result.add(callback);
                }
                byteArrayOutputStream.write(complexData.getData());
                if (byteArrayOutputStream.size() >= batchSize) {
                    outputStream.write(byteArrayOutputStream.toByteArray());
                    byteArrayOutputStream.reset();
                }
            }
            // Допустим сменился batchSize и мы не добрали на пачку, надо всё равно записать
            if (byteArrayOutputStream.size() > 0) {
                outputStream.write(byteArrayOutputStream.toByteArray());
            }
            if (onFlush != null) {
                onFlush.accept(result);
            }
            size.set(0);
            lock.set(false);
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try {
                flush();
            } finally {
                outputStream.close();
                closed = true;
            }
        }
    }

}