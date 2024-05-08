package ru.jamsys.core.util;

import java.util.function.Consumer;

public class ExpiredManagerEnvelope<T> {

    private final T value;
    private final Consumer<T> onExpired;

    public ExpiredManagerEnvelope(T value, Consumer<T> onExpired) {
        this.value = value;
        this.onExpired = onExpired;
    }

    public void expire(){
        this.onExpired.accept(this.value);
    }

}
