package ru.jamsys.extension;

import ru.jamsys.thread.ThreadEnvelope;

import java.util.concurrent.atomic.AtomicBoolean;

// Поддержка штанов, что бы функционал оставался жив
// Тут можно сбросить кеши / перепланировать работу - так что бы не сдохнуть)
public interface KeepAlive {

    void keepAlive(ThreadEnvelope threadEnvelope);

}
