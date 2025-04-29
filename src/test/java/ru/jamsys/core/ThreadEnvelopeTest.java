package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.flat.util.Util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// IO time: 4sec 32ms
// COMPUTE time: 4sec

class ThreadEnvelopeTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runSpring();
    }

    @AfterAll
    static void shutdown() {
        App.context = null;
    }

    @Test
    void test() {
        ConcurrentHashMap<Long, AvgMetric> map = new ConcurrentHashMap<>();

        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                while (map.size() < 3) {
                    long sec = Util.zeroLastNDigits(System.currentTimeMillis(), 3);
                    map.computeIfAbsent(sec, _ -> {
                        counter.incrementAndGet();
                        return new AvgMetric();
                    });
                    Thread.onSpinWait();
                }

            }).start();
        }
        Util.testSleepMs(4000);
        UtilLog.printInfo(ThreadEnvelopeTest.class, "map.size() = " + map.size() + "; counter: " + counter.get());
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals(3, counter.get());
    }



    @Test
    void test3() {
        ConcurrentHashMap<Long, AvgMetric> map = new ConcurrentHashMap<>();
        AvgMetric avgMetric = map.computeIfAbsent(1L, _ -> new AvgMetric());
        AvgMetric avgMetric2 = map.computeIfAbsent(1L, _ -> new AvgMetric());
        Assertions.assertEquals(avgMetric, avgMetric2);
    }

}