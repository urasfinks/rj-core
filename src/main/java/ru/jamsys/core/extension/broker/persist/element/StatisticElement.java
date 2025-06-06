package ru.jamsys.core.extension.broker.persist.element;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.ByteCodec;
import ru.jamsys.core.extension.async.writer.Position;

@Getter
@Setter
public class StatisticElement implements ByteCodec, Position {

    private long position;

    private String value;

    public StatisticElement(String value) {
        this.value = value;
    }

    @Override
    public byte[] toBytes() {
        return value.getBytes();
    }

    @Override
    public void fromBytes(byte[] bytes) {
        setValue(new String(bytes));
    }

}
