package ru.jamsys.scheduler;

import org.junit.jupiter.api.Test;
import ru.jamsys.Util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

class AbstractSchedulerThreadTest {

    @Test
    public void test() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        long delay = 1000;
        AtomicBoolean isRun = new AtomicBoolean(true);
        executorService.submit(() -> {
            Thread current = Thread.currentThread();
            long nextStart = System.currentTimeMillis();
            int count = 0;
            while (isRun.get()) {
                if (current.isInterrupted()) {
                    break;
                }
                nextStart = zeroLastNDigits(nextStart + delay, 3);
                Util.logConsole("xx");
                Util.sleepMillis(1100 - (count++ * 100L));
                long calcSleep = nextStart - System.currentTimeMillis();
                if (calcSleep > 0) {
                    Util.sleepMillis(calcSleep);
                } else {
                    Util.sleepMillis(1);//Что бы поймать Interrupt
                    nextStart = System.currentTimeMillis();
                }
            }
        });
        Thread.sleep(10000);
    }

    public static long zeroLastNDigits(long x, long n) {
        long tenToTheN = (long) Math.pow(10, n);
        return (x / tenToTheN) * tenToTheN;
    }

}