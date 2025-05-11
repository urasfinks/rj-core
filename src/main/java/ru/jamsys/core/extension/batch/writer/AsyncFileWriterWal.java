package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Consumer;

// Многопоточная запись в файл пачками (для Write-Ahead Logging)

@Getter
public class AsyncFileWriterWal<T extends AbstractAsyncFileWriterElement>
        extends AbstractAsyncFileWriter<T> {

    public AsyncFileWriterWal(
            ApplicationContext applicationContext,
            String ns,
            String filePath,
            Consumer<List<T>> onWrite
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