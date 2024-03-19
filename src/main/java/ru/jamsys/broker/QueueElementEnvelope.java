package ru.jamsys.broker;

import lombok.Getter;

public class QueueElementEnvelope<T> {

    @Getter
    long timeAdd = System.currentTimeMillis();

    @Getter
    T element;

    public QueueElementEnvelope(T element) {
        this.element = element;
    }

}
