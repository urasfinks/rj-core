package ru.jamsys.core.extension.expiration;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

import java.util.Map;

@Getter
@Setter
public class ExpirationMapExpirationObject {

    private final Object key;

    private Object value;

    private final Map<?, ?> map;

    private DisposableExpirationMsImmutableEnvelope<?> expiration;

    public ExpirationMapExpirationObject(Object key, Map<?, ?> map) {
        this.key = key;
        this.map = map;
    }

    public void remove() {
        map.remove(key);
    }

}
