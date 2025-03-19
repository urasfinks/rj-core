package ru.jamsys.core.extension.raw.writer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.ByteSerialization;

@Getter
@Accessors(chain = true)
public class BlockInfo<TX extends ByteSerialization> {

    @Setter
    private short writerFlag;

    private final long position; // Позиция начала блока

    @JsonIgnore
    private byte[] bytes;

    private final Class<TX> cls;

    private final int dataLength;

    public BlockInfo(
            long position,
            short writerFlag,
            int dataLength,
            Class<TX> cls
    ) {
        this.position = position;
        this.writerFlag = writerFlag;
        this.dataLength = dataLength;
        this.cls = cls;
    }

    public BlockInfo<TX> setBytes(byte[] bytes) {
        if (bytes.length != dataLength) {
            throw new RuntimeException("allocation byte size != current byte size");
        }
        this.bytes = bytes;
        return this;
    }

    @JsonProperty("data")
    public TX cast() throws Exception {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        TX item = cls.getConstructor().newInstance();
        item.toObject(bytes);
        item.setWriterFlag(writerFlag);
        return item;
    }

}
