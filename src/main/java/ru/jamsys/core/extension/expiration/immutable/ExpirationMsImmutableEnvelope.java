package ru.jamsys.core.extension.expiration.immutable;

import lombok.Getter;

@Getter
public class ExpirationMsImmutableEnvelope<T> extends ExpirationMsImmutableImpl {

    final T value;

    public ExpirationMsImmutableEnvelope(T value, long keepAliveOnInactivityMs) {
        super(keepAliveOnInactivityMs);
        this.value = value;
    }

    public ExpirationMsImmutableEnvelope(T value, long keepAliveOnInactivityMs, long lastActivityMs) {
        super(keepAliveOnInactivityMs, lastActivityMs);
        this.value = value;
    }

}
