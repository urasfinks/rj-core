package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.batch.writer.Position;

@Getter
public class X<T extends Position & ByteSerializable>
        implements Position, ByteSerializable {

    private final T element;

    @Setter
    private long position;

    public X(T element) {
        this.element = element;
    }

    @Override
    public byte[] toBytes() throws Exception {
        return element.toBytes();
    }

}
