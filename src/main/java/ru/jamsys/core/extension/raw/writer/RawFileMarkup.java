package ru.jamsys.core.extension.raw.writer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.ByteSerialization;

// Разметка для сырого чтения файла
// Изначально создаётся без данных, только позиция, флаг и длина.
// Впоследствии можно считать сами данные + скастовать в первоначальный объект

@Getter
@Accessors(chain = true)
public class RawFileMarkup<T extends ByteSerialization> {

    private final long position; // Позиция начала блока

    @JsonIgnore
    private byte[] bytes;

    private final Class<T> cls;

    private final int dataLength;

    public RawFileMarkup(
            long position,
            int dataLength,
            Class<T> cls
    ) {
        this.position = position;
        this.dataLength = dataLength;
        this.cls = cls;
    }

    public RawFileMarkup<T> setBytes(byte[] bytes) {
        if (bytes.length != dataLength) {
            throw new RuntimeException("allocation byte size != current byte size");
        }
        this.bytes = bytes;
        return this;
    }

    @JsonProperty("object")
    public T toObject() throws Exception {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        T item = cls.getConstructor().newInstance();
        item.toObject(bytes);
        return item;
    }

}
