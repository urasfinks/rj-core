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

// Однопоточная запись в файл пачками по 4кб

@Getter
public class BatchFileWriter implements AutoCloseable {

    @Setter
    private static int batchSize = (int) UtilByte.kilobytesToBytes(4); // 4KB

    private final ByteArrayOutputStream byteArrayOutputStream;

    private final OutputStream outputStream;

    private boolean closed;

    @SuppressWarnings("unused")
    public BatchFileWriter(String filePath) throws IOException {
        this(Paths.get(filePath));
    }

    public BatchFileWriter(Path filePath) throws IOException {
        this.byteArrayOutputStream = new ByteArrayOutputStream(batchSize);
        this.outputStream = Files.newOutputStream(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
                StandardOpenOption.DSYNC
        );
        this.closed = false;
    }

    public void write(@NotNull byte[] data) throws IOException {
        if (closed) {
            throw new IOException("Writer is closed");
        }
        byteArrayOutputStream.write(data);
        if (byteArrayOutputStream.size() >= batchSize) {
            flush();
        }
    }

    private void flush() throws IOException {
        if (byteArrayOutputStream.size() > 0) {
            outputStream.write(byteArrayOutputStream.toByteArray());
            byteArrayOutputStream.reset();
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