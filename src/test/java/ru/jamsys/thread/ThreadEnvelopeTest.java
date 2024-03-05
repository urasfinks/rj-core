package ru.jamsys.thread;

import org.junit.jupiter.api.Test;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

class ThreadEnvelopeTest {
    @Test
    public void test() {
        ThreadEnvelope threadEnvelope = new ThreadEnvelope((AtomicBoolean isWhile, ThreadEnvelope te) -> {
            Util.logConsole("YHOO");
            return false;
        });
        threadEnvelope.run();
        Util.sleepMs(1000);
        threadEnvelope.resume();
        Util.sleepMs(1000);
        threadEnvelope.resume();
        Util.sleepMs(1000);
        threadEnvelope.shutdown();
    }
}