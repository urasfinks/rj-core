package ru.jamsys.core.extension.raw.writer;

// Горячая замена
// Если есть несколько источников данных, и хочется бесшовно с ними работать, не думая о том что там список ресурсов.

// T - менаджер элементов
// TE - элемент

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractHotSwap<T extends Completed<TE>, TE> implements HotSwap<T> {

    protected volatile T primary;
    private final AtomicInteger seq = new AtomicInteger(0);
    private final AtomicLong timeNextSwap = new AtomicLong(0);
    private final AtomicBoolean onSwap = new AtomicBoolean(true);

    public AbstractHotSwap() {
        this.primary = getNextSwap(seq.getAndIncrement());
    }

    // Попытки замены не должны быть больше 1 раза в секунду
    protected void swap() {
        if (System.currentTimeMillis() < timeNextSwap.get()) {
            return;
        }
        if (onSwap.compareAndSet(true, false)) {
            try {
                if (System.currentTimeMillis() >= timeNextSwap.get()) {
                    if (primary == null || primary.isCompleted()) {
                        T old = primary;
                        primary = getNextSwap(seq.getAndIncrement());
                        if (old != null) {
                            old.release();
                        }
                    }
                }
                timeNextSwap.set(System.currentTimeMillis() + 1000);
            } finally {
                onSwap.set(true);
            }
        }
    }

    public TE getResource() {
        if (primary == null || primary.isCompleted()) {
            swap();
        }
        if (primary != null && !primary.isCompleted()) {
            return primary.getIfNotCompleted();
        }
        return null;
    }

}
