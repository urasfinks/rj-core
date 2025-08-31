package ru.jamsys.core.extension.expiration;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

import java.util.Map;

@Getter
@Setter
public class EnvelopeObject {

    private final Object key;

    private Object value;

    private final Map<?, ?> map;

    private DisposableExpirationMsImmutableEnvelope<?> expiration;

    // Нельзя прихранивать expirationList
    public EnvelopeObject(
            Object key,
            Object value,
            Map<?, ?> map
    ) {
        this.key = key;
        this.value = value;
        this.map = map;
    }

    // Нельзя прихранивать expirationList
    public void updateExpiration(ExpirationList<EnvelopeObject> expirationList, int timeOut) {
        if (expiration != null) {
            expiration.doNeutralized();
        }
        expiration = expirationList.add(this, timeOut);
    }

    public void remove() {
        Object remove = map.remove(key);
        if (remove instanceof ExpirationDrop expirationDrop) {
            try {
                expirationDrop.onExpirationDrop();
            } catch (Throwable th) {
                App.error(th);
            }
        }
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("key", key)
                .append("value", value)
                ;
    }

}
