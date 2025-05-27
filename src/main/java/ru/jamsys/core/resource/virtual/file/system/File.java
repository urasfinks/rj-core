package ru.jamsys.core.resource.virtual.file.system;

import lombok.Getter;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ConsumerThrowing;
import ru.jamsys.core.extension.functional.SupplierThrowing;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.flat.util.UtilUri;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class File extends AbstractManagerElement {

    final private UtilUri.FilePath filePath;

    private SupplierThrowing<byte[]> readFromSource;

    private ConsumerThrowing<byte[]> writeToDestination;

    protected volatile byte[] bytes;

    public File(String path) {
        this.filePath = UtilUri.parsePath(path);
    }

    public void setupTimeoutMs(int keepAliveOnInactivityMs) {
        setKeepAliveOnInactivityMs(keepAliveOnInactivityMs);
    }

    public void setupReadFromSource(SupplierThrowing<byte[]> readFromSource) {
        this.readFromSource = readFromSource;
    }

    public void setupWriteToDestination(ConsumerThrowing<byte[]> writeToDestination) {
        this.writeToDestination = writeToDestination;
    }

    public void flush() throws Throwable {
        if (writeToDestination == null) {
            throw new Exception("WriteToDestination is null File: " + filePath.getPath());
        }
        writeToDestination.accept(getBytes());
    }

    public void reloadBytes() {
        try {
            bytes = readFromSource.get();
        } catch (Throwable th) {
            throw new ForwardException(filePath.getPath(), th);
        }
    }

    @Override
    public List<DataHeader> flushAndGetStatistic( AtomicBoolean threadRun) {
        return List.of();
    }

    @Override
    public void runOperation() {
        if (readFromSource == null) {
            throw new RuntimeException("ReadFromSource is null File: " + filePath.getPath());
        }
        reloadBytes();
    }

    @Override
    public void shutdownOperation() {
        bytes = null;
    }

}
