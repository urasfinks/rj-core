package ru.jamsys.core.extension.batch.writer;

public abstract class AbstractAsyncFileWriterElement {

    abstract void setPosition(long andAdd);

    abstract byte[] getBytes();

}
