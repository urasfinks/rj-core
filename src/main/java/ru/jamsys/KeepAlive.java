package ru.jamsys;

import java.util.concurrent.atomic.AtomicBoolean;

public interface KeepAlive {
    void keepAlive(AtomicBoolean isRun);
}
