package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.batch.writer.AbstractAsyncFileWriterElement;
import ru.jamsys.core.flat.util.UtilByte;

@Getter
@Setter
public class CommitElement extends AbstractAsyncFileWriterElement {

    private final BrokerPersistElement<? extends ByteSerialization> bin; // Это блок данных .afwr

    private long position; // Это позиция записанных данных в .commit (нам не понадобится)

    public CommitElement(BrokerPersistElement<? extends ByteSerialization> bin) {
        this.bin = bin;
    }

    @Override
    public void setFilePath(String fileName) {}

    @Override
    public byte[] getBytes() throws Exception {
        return UtilByte.longToBytes(bin.getPosition());
    }

}
