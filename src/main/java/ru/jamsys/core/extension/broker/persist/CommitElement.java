package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.batch.writer.AbstractAsyncFileWriterElement;
import ru.jamsys.core.flat.util.UtilByte;

@Getter
@Setter
public class CommitElement extends AbstractAsyncFileWriterElement {

    private final long key; // Это позиция оригинального блока данных .afwr

    private long position; // Это позиция записанных данных в .wal

    public CommitElement(long key) {
        this.key = key;
    }

    @Override
    public void setFilePath(String fileName) {

    }

    @Override
    public byte[] getBytes() throws Exception {
        return UtilByte.longToBytes(key);
    }

}
