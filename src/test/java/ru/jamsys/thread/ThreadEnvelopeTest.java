package ru.jamsys.thread;

import org.junit.jupiter.api.Test;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

class ThreadEnvelopeTest {
    @Test
    public void test() {

        ThreadPool threadPool = new ThreadPool("Generator", 1, 1, 60000, (AtomicBoolean isWhile) -> {
            Thread currentThread = Thread.currentThread();
            long nextStartMs = System.currentTimeMillis();
            while (isWhile.get() && !currentThread.isInterrupted()) {
                nextStartMs = Util.zeroLastNDigits(nextStartMs + 1000, 3);

                if (isWhile.get()) {
                    long calcSleepMs = nextStartMs - System.currentTimeMillis();
                    if (calcSleepMs > 0) {
                        Util.sleepMs(calcSleepMs);
                    } else {
                        Util.sleepMs(1);//Что бы поймать Interrupt
                        nextStartMs = System.currentTimeMillis();
                    }
                } else {
                    break;
                }
            }
            Util.logConsole(currentThread.getName() + ": STOP");
            return false;
        });
        threadPool.run();
        Util.sleepMs(10000);

//        ThreadEnvelope threadEnvelope = new ThreadEnvelope();
//        threadEnvelope.run();
//        Util.sleepMs(1000);
//        threadEnvelope.resume();
//        Util.sleepMs(1000);
//        threadEnvelope.resume();
//        Util.sleepMs(1000);
//        threadEnvelope.shutdown();
    }
}