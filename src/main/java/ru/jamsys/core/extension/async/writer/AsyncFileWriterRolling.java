package ru.jamsys.core.extension.async.writer;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.broker.persist.BrokerPersistRepositoryProperty;

import java.nio.file.StandardOpenOption;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

// Многопоточная запись в файл пачками в цикличном режиме

@Getter
public class AsyncFileWriterRolling<T extends Position & ByteSerializable>
        extends AbstractAsyncFileWriter<T> {

    private String fileName;

    private BiConsumer<String, AsyncFileWriterRolling<T>> onFileSwap;

    @Setter
    private Supplier<String> generateNewFileName = () -> System.currentTimeMillis()
            + "_"
            + java.util.UUID.randomUUID()
            + ".afwr";

    @SuppressWarnings("all")
    public AsyncFileWriterRolling(String ns, String key) { // Тут fileName нужен только для поддержания контракта ManagerElement
        super(null, key);
        super.setupStandardOpenOption(StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public void setupRepositoryProperty(BrokerPersistRepositoryProperty repositoryProperty) {
        super.setupRepositoryProperty(repositoryProperty);

        // До run надо установить имя файла, так как при старте будет создаваться файл и если этого не сделать будет NPE
        setNewFilePath();

        // При превышении размера, будем вызывать замену имени файла
        setOnOutOfPosition(() -> {
            setNewFilePath();
            restartOutputStream();
            onFileSwap.accept(fileName, this);
        });
    }

    public void setupOnFileSwap(BiConsumer<String, AsyncFileWriterRolling<T>> onFileSwap) {
        this.onFileSwap = onFileSwap;
    }

    @Override
    public void setupStandardOpenOption(StandardOpenOption standardOpenOption) {
        throw new RuntimeException("Only StandardOpenOption.TRUNCATE_EXISTING");
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