package ru.jamsys.broker;

public class ElementWrap<T> {

    private final long timestamp = System.currentTimeMillis();
    private final T element;

    public ElementWrap(T element) {
        this.element = element;
    }

    public T getElement() {
        return element;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
