package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.batch.writer.AbstractAsyncFileWriterElement;

@Getter
public  class BrokerPersistElement<T extends ByteSerialization> extends AbstractAsyncFileWriterElement {

    private final T element;

    private long position;

    public BrokerPersistElement(T element) {
        this.element = element;
    }

    @Override
    public void setPosition(long position) {
        this.position = position;
    }

    @Override
    public byte[] getBytes() throws Exception {
        return element.toByte();
    }

}
