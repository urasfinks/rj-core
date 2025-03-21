package ru.jamsys.core.extension.raw.writer;

// Горячая замена
// Если есть несколько источников данных, и хочется бесшовно с ними работать, не думая о том что там список ресурсов.

// T - менаджер элементов
// TE - элемент

import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public abstract class AbstractHotSwap<T extends Completable> implements HotSwap<T> {

    protected volatile T resource;
    private final AtomicInteger seq = new AtomicInteger(0);
    private final AtomicLong timeNextSwap = new AtomicLong(0);
    private final AtomicBoolean lock = new AtomicBoolean(true);

    @Setter
    private Consumer<T> onSwap;

    public AbstractHotSwap() {
        this.resource = getNextHotSwap(seq.getAndIncrement());
    }

    // Попытки замены не должны быть больше 1 раза в секунду
    protected void swap() {
        if (System.currentTimeMillis() < timeNextSwap.get()) {
            return;
        }
        if (lock.compareAndSet(true, false)) {
            try {
                if (System.currentTimeMillis() >= timeNextSwap.get()) {
                    if (resource == null || resource.isCompleted()) {
                        T old = resource;
                        resource = getNextHotSwap(seq.getAndIncrement());
                        if (old != null && onSwap != null) {
                            onSwap.accept(old);
                        }
                    }
                }
                timeNextSwap.set(System.currentTimeMillis() + 1000);
            } finally {
                lock.set(true);
            }
        }
    }

    public T getResource() {
        if (resource == null || resource.isCompleted()) {
            swap();
        }
        return resource;
    }

}
