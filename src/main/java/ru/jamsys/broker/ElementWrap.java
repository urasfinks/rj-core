package ru.jamsys.broker;

import lombok.NonNull;

public class ElementWrap<T> {

    long timestamp = System.currentTimeMillis();
    T element;

    public ElementWrap(T element) {
        this.element = element;
    }

    public T getElement() {
        return element;
    }

    @NonNull
    public long getTimestamp() {
        return timestamp;
    }

}
