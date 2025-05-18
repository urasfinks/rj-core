package ru.jamsys.core.extension.async.writer;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

// Класс данных полезной нагрузки, что бы не пересериализовывать данные туда и обратно
@Getter
@Setter
public class DataPayload {

    private long position;

    private byte[] bytes;

    private Object object;

    // CAS избыточен, так как всё последовательно идёт, сначала poll потом remove
    private volatile boolean remove = false;

    private DisposableExpirationMsImmutableEnvelope<DataPayload> expiration;

    public DataPayload(long position, byte[] bytes, Object object) {
        this.position = position;
        this.bytes = bytes;
        this.object = object;
    }

}
