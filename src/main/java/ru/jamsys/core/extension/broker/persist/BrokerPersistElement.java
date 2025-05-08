package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.batch.writer.AbstractAsyncFileWriterElement;

@Getter
public  class BrokerPersistElement<T extends ByteSerialization> extends AbstractAsyncFileWriterElement {

    private final T element;

    @Setter
    private long position;

    @Setter
    private String filePath;

    public BrokerPersistElement(T element) {
        this.element = element;
    }

    @Override
    public byte[] getBytes() throws Exception {
        return element.toByte();
    }

}
