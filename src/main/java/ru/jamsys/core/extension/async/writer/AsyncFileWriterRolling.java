package ru.jamsys.core.extension.async.writer;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.broker.BrokerPersistRepositoryProperty;

import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

// Многопоточная запись в файл пачками в цикличном режиме

@Getter
public class AsyncFileWriterRolling<T extends Position & ByteSerializable>
        extends AbstractAsyncFileWriter<T> {

    private String fileName;

    private final BiConsumer<String, AsyncFileWriterRolling<T>> onFileSwap;

    @Setter
    private Supplier<String> generateNewFileName = () -> System.currentTimeMillis()
            + "_"
            + java.util.UUID.randomUUID()
            + ".afwr";

    public AsyncFileWriterRolling(
            BrokerPersistRepositoryProperty repositoryProperty,
            BiConsumer<String, List<T>> onWrite, // T - filePath; U - list written object
            BiConsumer<String, AsyncFileWriterRolling<T>> onFileSwap // T - fileName
    ) {
        super(repositoryProperty, null, onWrite, StandardOpenOption.TRUNCATE_EXISTING);
        this.onFileSwap = onFileSwap;

        // До run надо установить имя файла, так как при старте будет создаваться файл и если этого не сделать будет NPE
        setNewFilePath();

        // При превышении размера, будем вызывать замену имени файла
        setOnOutOfPosition(() -> {
            setNewFilePath();
            restartOutputStream();
            onFileSwap.accept(fileName, this);
        });
    }

    private void setNewFilePath() {
        fileName = generateNewFileName.get();
        setFilePath(getRepositoryProperty().getDirectory() + "/" + fileName);
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
        onFileSwap.accept(fileName, this);
    }

}