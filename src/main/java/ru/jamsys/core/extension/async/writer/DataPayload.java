package ru.jamsys.core.extension.async.writer;

import lombok.Getter;
import lombok.Setter;

// Класс данных полезной нагрузки, что бы не пересериализовывать данные туда и обратно
@Getter
@Setter
public class DataPayload {

    private long position;

    private byte[] bytes;

    private Object object;

    public DataPayload(long position, byte[] bytes, Object object) {
        this.position = position;
        this.bytes = bytes;
        this.object = object;
    }

}
