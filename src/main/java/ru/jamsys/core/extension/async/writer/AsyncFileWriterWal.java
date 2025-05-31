package ru.jamsys.core.extension.async.writer;

import lombok.Getter;
import ru.jamsys.core.extension.ByteSerializable;

import java.nio.file.StandardOpenOption;

// Многопоточная запись в файл пачками (для Write-Ahead Logging)

@Getter
public class AsyncFileWriterWal<T extends Position & ByteSerializable>
        extends AbstractAsyncFileWriter<T> {

    public AsyncFileWriterWal(String ns) {
        super(ns);
        super.setupStandardOpenOption(StandardOpenOption.APPEND);
    }

    @Override
    public void setupStandardOpenOption(StandardOpenOption standardOpenOption) {
        throw new RuntimeException("Only StandardOpenOption.APPEND");
    }

    @Override
    public void writeAsync(T data) {
        if (!isRun()) {
            throw new RuntimeException("Writer is closed");
        }
        getInputQueue().add(data);
    }

}