package ru.jamsys.core.extension.expiration;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

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
        Object remove = map.remove(key);
        if (remove instanceof Expiration expiration1) {
            try {
                expiration1.onExpired();
            } catch (Throwable th) {
                App.error(th);
            }
        }
    }

}
