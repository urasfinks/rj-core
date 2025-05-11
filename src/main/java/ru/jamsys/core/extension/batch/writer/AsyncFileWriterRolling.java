package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;

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

        // До run надо установить имя файла, так как при старте будет создаваться файл и если этого не сделать будет NPE
        setNewFilePath();

        // При превышении размера, будем вызывать замену имени файла
        setOnOutOfPosition(() -> {
            setNewFilePath();
            restartOutputStream();
            onSwap.accept(fileName);
        });

    }

    private void setNewFilePath() {
        fileName = generateNewFileName.get();
        setFilePath(directory + "/" + fileName);
    }

    // Переопределяем, чтобы не получить Exception на вставке, при выходе за границы maxPosition
    @Override
    public void writeAsync(T data) {
        if (!isRun()) {
            throw new RuntimeException("Writer is closed");
        }
        getInputQueue().add(data);
    }

    @Override
    public void runOperation() {
        super.runOperation();
        // Это просто запуск, но в конструкторе мы не вызывали onSwap, потому что реально файл создаётся только в
        // super.runOperation(), а onSwap по семантике должен вызываться, после того, как файл будет создан и замещён
        onSwap.accept(fileName);
    }

}