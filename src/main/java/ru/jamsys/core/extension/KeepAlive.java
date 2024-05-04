package ru.jamsys.core.extension;

import java.util.concurrent.atomic.AtomicBoolean;

// Поддержка штанов, что бы функционал оставался жив
// Тут можно сбросить кеши / перепланировать работу - так что бы не сдохнуть)
public interface KeepAlive {

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    void keepAlive(AtomicBoolean isThreadRun);

}
