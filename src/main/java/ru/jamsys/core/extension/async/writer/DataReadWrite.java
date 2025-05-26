package ru.jamsys.core.extension.async.writer;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

// Класс данных полезной нагрузки, что бы не пересериализовывать данные туда и обратно
@Getter
@Setter
public class DataReadWrite {

    private long position; // Позиция появляется в момент записи или чтения из файла

    private byte[] bytes;

    private Object object;

    // CAS избыточен, так как всё последовательно идёт, сначала poll потом remove
    private volatile boolean remove = false;

    private DisposableExpirationMsImmutableEnvelope<?> expiration;

    public DataReadWrite(long position, byte[] bytes, Object object) {
        this.position = position;
        this.bytes = bytes;
        this.object = object;
    }

}
