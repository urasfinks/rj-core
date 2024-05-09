package ru.jamsys.core.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.item.Session;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableEnvelope;
import ru.jamsys.core.util.Util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class CacheTest {

    AtomicBoolean isThreadRun = new AtomicBoolean(true);
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @Test
    void add() {
        Session<Integer, String> cache = new Session<>("test");

        cache.add(1234, "Hello world", 100);
        Assertions.assertEquals(1, cache.getMap().size(), "#1");

        cache.add(12345, "Hello world", 100);
        Assertions.assertEquals(2, cache.getMap().size(), "#2");

        cache.add(123456, "Hello world", 1000);
        Assertions.assertEquals(3, cache.getMap().size(), "#4");

        Util.sleepMs(200);

        cache.keepAlive(isThreadRun);

        cache.add(1234567, "Hello world", 100);
        Assertions.assertEquals(2, cache.getMap().size(), "#5");
    }



//    Map<String, Object> multiThread(int sleepKeepAlive, int timeoutMs) {
//        Session<Integer, String> cache = new Session<>("test");
//        AvgMetric avgMetric = new AvgMetric();
//        cache.setOnExpired((ExpiredMsMutableEnvelope<String> env) -> {
//            if (env.getExpiryRemainingMs() > 0) {
//                Assertions.fail("ALARM");
//            } else {
//                avgMetric.add(env.getExpiryRemainingMs() * -1);
//            }
//        });
//        AtomicBoolean isRun = new AtomicBoolean(true);
//        AtomicBoolean isRun2 = new AtomicBoolean(true);
//        AtomicInteger counter = new AtomicInteger(0);
//
//
//
//        //Сначала надо запустить keepAlive потому что старт потоков будет медленный и мы начнём терять секунды так как не запущенны
//        new Thread(() -> {
//            while (isRun2.get()) {
//                long cur = System.currentTimeMillis();
//                Session.KeepAliveResult keepAliveResult = cache.keepAlive(isThreadRun, cur);
//                System.out.println(keepAliveResult);
//                Util.sleepMs(sleepKeepAlive);
//            }
//        }).start();
//
//        for (int i = 0; i < 4; i++) {
//            final int x = i;
//            Util.sleepMs(333 * i); // Сделаем рассинхрон вставок по времени
//            new Thread(() -> {
//                while (isRun.get()) {
//                    for (int j = 0; j < 1000; j++) {
//                        try {
//                            cache.add(counter.getAndIncrement(), java.util.UUID.randomUUID().toString(), timeoutMs);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    Util.sleepMs((100 * x) + 10);
//                }
//            }).start();
//        }
//
//
//        Util.sleepMs(5000);
//        isRun.set(false);
//        Util.sleepMs(timeoutMs + sleepKeepAlive);
//        isRun2.set(false);
//        Map<String, Object> flush = avgMetric.flush("");
//        Map<String, Object> fields = cache.flushAndGetStatistic(null, null, null).getFirst().getFields();
//        flush.putAll(fields);
//        System.out.println(flush);
//        Assertions.assertEquals(0, flush.get("MapSize"));
//        Assertions.assertEquals(0, flush.get("BucketSize"));
//        return flush;
//    }

//    @Test
//    void runThreadAny() {
//        //{AvgCount=5357983, Min=-276, Max=-1, Sum=-449148926, Avg=-83.82798638965447, MapSize=0, BucketSize=0}
//
//        Map<String, Object> stat = multiThread(100, 500);
//        double timeAvg = (double) stat.get("Avg");
//        Assertions.assertTrue(timeAvg <= 100);
//
//        stat = multiThread(100, 1000);
//        timeAvg = (double) stat.get("Avg");
//        Assertions.assertTrue(timeAvg <= 100);
//
//        stat = multiThread(200, 600);
//        timeAvg = (double) stat.get("Avg");
//        Assertions.assertTrue(timeAvg <= 200);
//
//        stat = multiThread(600, 600);
//        timeAvg = (double) stat.get("Avg");
//        Assertions.assertTrue(timeAvg <= 600);
//
//        stat = multiThread(1200, 600);
//        timeAvg = (double) stat.get("Avg");
//        Assertions.assertTrue(timeAvg <= 1200);
//
//        stat = multiThread(2400, 600);
//        timeAvg = (double) stat.get("Avg");
//        Assertions.assertTrue(timeAvg <= 2400);
//
//        stat = multiThread(4800, 600);
//        timeAvg = (double) stat.get("Avg");
//        Assertions.assertTrue(timeAvg <= 4800);
//
//    }

}