package ru.jamsys.core.extension.raw.writer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.ByteSerialization;

// Разметка для сырого чтения файла
// Изначально создаётся без данных, только позиция, флаг и длина.
// Впоследствии можно считать сами данные + скастовать в первоначальный объект

@Getter
@Accessors(chain = true)
public class RawFileBlock<T extends ByteSerialization> {

    @Setter
    private short writerFlag;

    private final long position; // Позиция начала блока

    @JsonIgnore
    private byte[] bytes;

    private final Class<T> cls;

    private final int dataLength;

    public RawFileBlock(
            long position,
            short writerFlag,
            int dataLength,
            Class<T> cls
    ) {
        this.position = position;
        this.writerFlag = writerFlag;
        this.dataLength = dataLength;
        this.cls = cls;
    }

    public RawFileBlock<T> setBytes(byte[] bytes) {
        if (bytes.length != dataLength) {
            throw new RuntimeException("allocation byte size != current byte size");
        }
        this.bytes = bytes;
        return this;
    }

    @JsonProperty("data")
    public T cast() throws Exception {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        T item = cls.getConstructor().newInstance();
        item.toObject(bytes);
        item.setWriterFlag(writerFlag);
        return item;
    }

}
