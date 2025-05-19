package ru.jamsys.core.extension.async.writer;

import lombok.Getter;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.broker.BrokerPersistRepositoryProperty;

import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.BiConsumer;

// Многопоточная запись в файл пачками (для Write-Ahead Logging)

@Getter
public class AsyncFileWriterWal<T extends Position & ByteSerializable>
        extends AbstractAsyncFileWriter<T> {

    public AsyncFileWriterWal(
            BrokerPersistRepositoryProperty repositoryProperty,
            String filePath,
            BiConsumer<String, List<T>> onWrite
    ) {
        super(repositoryProperty, filePath, onWrite, StandardOpenOption.APPEND);
    }

    @Override
    public void writeAsync(T data) {
        markActive();
        if (!isRun()) {
            throw new RuntimeException("Writer is closed");
        }
        getInputQueue().add(data);
    }

}