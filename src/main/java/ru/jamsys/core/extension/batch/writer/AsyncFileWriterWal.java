package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import ru.jamsys.core.extension.ByteSerializable;

import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.BiConsumer;

// Многопоточная запись в файл пачками (для Write-Ahead Logging)

@Getter
public class AsyncFileWriterWal<T extends Position & ByteSerializable>
        extends AbstractAsyncFileWriter<T> {

    public AsyncFileWriterWal(
            ApplicationContext applicationContext,
            String ns,
            String filePath,
            BiConsumer<String, List<T>> onWrite
    ) {
        super(applicationContext, ns, filePath, onWrite, StandardOpenOption.APPEND);
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