package ru.jamsys.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class ExpiredManagerTest {

    @Test
    void add() throws InterruptedException {
        ExpiredManager<String> expiredManager = new ExpiredManager<>();
        long timestampOut = System.currentTimeMillis() + 1000;
        for (int i = 0; i < 10; i++) {
            expiredManager.add(java.util.UUID.randomUUID().toString(), timestampOut);
        }
        Thread.sleep(1000);
        List<String> expired = expiredManager.getExpired();
        Assertions.assertEquals(0, expiredManager.getCountBucket());
        Assertions.assertEquals(10, expired.size());

        AtomicInteger c = new AtomicInteger();
        AtomicBoolean run = new AtomicBoolean(true);
        for (int i = 0; i < 140; i++) {
            new Thread(() -> {
                while (run.get()) {
                    long timestampOut2 = System.currentTimeMillis() + 1000;
                    expiredManager.add(java.util.UUID.randomUUID().toString(), timestampOut2);
                    c.incrementAndGet();
                }
            }).start();
        }
        new Thread(() -> {
            while (run.get()) {
                List<String> expired1 = expiredManager.getExpired();
                System.out.println(expired1.size());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        Thread.sleep(15000);
        run.set(false);
        expired = expiredManager.getExpired();
        System.out.println(expired.size());
        System.out.println("All insert: " + c.incrementAndGet());
    }
}