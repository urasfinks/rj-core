package ru.jamsys.core.resource.virtual.file.system;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.UniversalPath;
import ru.jamsys.core.extension.functional.ConsumerThrowing;
import ru.jamsys.core.extension.functional.SupplierThrowing;

@Getter
public class File extends AbstractManagerElement {

    final private UniversalPath universalPath;

    private SupplierThrowing<byte[]> readFromSource;

    private ConsumerThrowing<byte[]> writeToDestination;

    protected volatile byte[] bytes;

    private final String key;

    public File(String ns, String key) {
        this.universalPath = new UniversalPath(ns);
        this.key = key;
    }

    public void setupTimeoutMs(int keepAliveOnInactivityMs) {
        this.setInactivityTimeoutMs(keepAliveOnInactivityMs);
    }

    public void setupReadFromSource(SupplierThrowing<byte[]> readFromSource) {
        this.readFromSource = readFromSource;
    }

    public void setupWriteToDestination(ConsumerThrowing<byte[]> writeToDestination) {
        this.writeToDestination = writeToDestination;
    }

    public void flush() throws Throwable {
        if (writeToDestination == null) {
            throw new Exception("WriteToDestination is null File: " + universalPath.getPath());
        }
        writeToDestination.accept(getBytes());
    }

    public void reloadBytes() {
        try {
            bytes = readFromSource.get();
        } catch (Throwable th) {
            throw new ForwardException(universalPath, th);
        }
    }

    @Override
    public void runOperation() {
        if (readFromSource == null) {
            throw new RuntimeException("ReadFromSource is null File: " + universalPath.getPath());
        }
        reloadBytes();
    }

    @Override
    public void shutdownOperation() {
        bytes = null;
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("filePath", universalPath)
                ;
    }

}
