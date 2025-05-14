package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.extension.ByteSerializable;

import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Многопоточная запись в файл пачками в цикличном режиме

@Getter
public class AsyncFileWriterRolling<T extends Position & ByteSerializable>
        extends AbstractAsyncFileWriter<T> {

    private final String directory;

    private String fileName;

    private final Consumer<String> onFileSwap;

    @Setter
    private Supplier<String> generateNewFileName = () -> System.currentTimeMillis()
            + "_"
            + java.util.UUID.randomUUID()
            + ".afwr";

    public AsyncFileWriterRolling(
            ApplicationContext applicationContext,
            String ns,
            String directory,
            BiConsumer<String, List<T>> onWrite, // T - filePath; U - list written object
            Consumer<String> onFileSwap // T - fileName
    ) {
        super(applicationContext, ns, null, onWrite, StandardOpenOption.TRUNCATE_EXISTING);
        this.directory = directory;
        this.onFileSwap = onFileSwap;

        // До run надо установить имя файла, так как при старте будет создаваться файл и если этого не сделать будет NPE
        setNewFilePath();

        // При превышении размера, будем вызывать замену имени файла
        setOnOutOfPosition(() -> {
            setNewFilePath();
            restartOutputStream();
            onFileSwap.accept(fileName);
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
        onFileSwap.accept(fileName);
    }

}