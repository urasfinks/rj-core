package ru.jamsys.core.extension.broker.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.async.writer.Position;

// После того, как всё будет отлажено переименовать в более подходящее имя.
@Getter
public class X<T extends Position & ByteSerializable>
        implements Position, ByteSerializable, RiderConfiguration {

    private final T element;

    @Setter
    private long position;

    public X(T element) {
        this.element = element;
    }

    @JsonIgnore
    @Setter
    private Manager.Configuration<Rider> riderConfiguration;

    @Override
    public byte[] toBytes() throws Exception {
        return element.toBytes();
    }

}
