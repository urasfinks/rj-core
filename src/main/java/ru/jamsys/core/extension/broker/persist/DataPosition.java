package ru.jamsys.core.extension.broker.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.experimental.Accessors;

// Позиция данных в файле .log
// Изначально создаётся без данных, только позиция, флаг и длина

@Getter
@Accessors(chain = true)
public class DataPosition {

    private final long position; // Позиция начала блока

    @JsonIgnore
    private byte[] bytes;

    private final int length;

    public DataPosition(
            long position,
            int length
    ) {
        this.position = position;
        this.length = length;
    }

    public DataPosition setBytes(byte[] bytes) {
        this.bytes = bytes;
        return this;
    }

}
