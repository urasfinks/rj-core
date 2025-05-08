package ru.jamsys.core.extension.batch.writer;

public abstract class AbstractAsyncFileWriterElement {

    abstract public void setPosition(long andAdd);

    abstract public void setFilePath(String fileName);

    abstract public byte[] getBytes() throws Exception;

}
