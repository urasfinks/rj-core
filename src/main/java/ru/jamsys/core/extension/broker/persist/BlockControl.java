package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.async.writer.Position;
import ru.jamsys.core.flat.util.UtilByte;

@Getter
@Setter
public class BlockControl implements ByteSerializable, Position {

    private final Position data; // Это блок данных .afwr

    private long position; // Это позиция записанных данных в .commit (нам не понадобится)

    public BlockControl(Position data) {
        this.data = data;
    }

    @Override
    public byte[] toBytes() {
        return UtilByte.longToBytes(data.getPosition());
    }

}
