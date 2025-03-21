package ru.jamsys.core.extension.raw.writer;

public interface HotSwap<T> {
    T getNextHotSwap(int seqNumber);
}
