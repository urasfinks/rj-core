package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.async.writer.Position;
import ru.jamsys.core.flat.util.UtilByte;

// TODO: После того, как всё будет отлажено переименовать в более подходящее имя.
@Getter
@Setter
public class Y implements ByteSerializable, Position {

    private final Position x; // Это блок данных .afwr

    private long position; // Это позиция записанных данных в .commit (нам не понадобится)

    public Y(Position x) {
        this.x = x;
    }

    @Override
    public byte[] toBytes() {
        return UtilByte.longToBytes(x.getPosition());
    }

}
