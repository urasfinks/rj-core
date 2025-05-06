package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Многопоточная запись в файл пачками в цикличном режиме

@Getter
public class AsyncFileWriterRolling<T extends AbstractAsyncFileWriterElement>
        extends AbstractAsyncFileWriter<T> {

    private final String directory;

    private String fileName;

    private final Consumer<String> onSwap;

    @Setter
    private Supplier<String> generateNewFileName = () -> System.currentTimeMillis()
            + "_"
            + java.util.UUID.randomUUID()
            + ".afwr";

    public AsyncFileWriterRolling(
            ApplicationContext applicationContext,
            String ns,
            String directory,
            Consumer<List<T>> onWrite,
            Consumer<String> onSwap // T - fileName
    ) {
        super(applicationContext, ns, null, onWrite, StandardOpenOption.TRUNCATE_EXISTING);
        this.directory = directory;
        this.onSwap = onSwap;
        setOnOutOfPosition(this::swap);
        swap();
    }

    private void swap() {
        fileName = generateNewFileName.get();
        setFilePath(directory + "/" + fileName);
        onSwap.accept(fileName);
        restartOutputStream();
    }

    // Переопределяем, чтобы не получить Exception на вставке, при выходе за границы maxPosition
    @Override
    public void writeAsync(T data) throws Throwable {
        if (!isRun()) {
            throw new IOException("Writer is closed");
        }
        getInputQueue().add(data);
    }

}