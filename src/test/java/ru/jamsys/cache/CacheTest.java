package ru.jamsys.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.component.item.Cache;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.TimeEnvelope;
import ru.jamsys.thread.TestThreadEnvelope;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.util.Util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class CacheTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.context = SpringApplication.run(App.class, args);
    }

    @Test
    void add() throws Exception {
        Cache<Integer, String> cache = new Cache<>();

        cache.add(1234, "Hello world", 100);
        Assertions.assertEquals(1, cache.getMap().size(), "#1");

        cache.add(12345, "Hello world", 100);
        Assertions.assertEquals(2, cache.getMap().size(), "#2");

        cache.add(123456, "Hello world", 1000);
        Assertions.assertEquals(3, cache.getMap().size(), "#4");

        Util.sleepMs(200);

        cache.keepAlive(TestThreadEnvelope.get());

        cache.add(1234567, "Hello world", 100);
        Assertions.assertEquals(2, cache.getMap().size(), "#5");
    }

    @Test
    void checkSize() throws Exception {
        ThreadEnvelope threadEnvelope = TestThreadEnvelope.get();
        long curTimeMs = 1709734264056L; //2024-03-06T17:11:04.056
        Cache<Integer, String> cache = new Cache<>();

        //2024-03-06T17:11:04.056 + 0 => 11:04.056 + 1000 => 11:05.056 => 11:05.000
        cache.add(0, "Hello world", curTimeMs, 1000);

        //Проверяем что пока 1 корзина
        Assertions.assertEquals("[1709734265000]", cache.getBucketKey().toString());
        Assertions.assertEquals("2024-03-06T17:11:05.000", Util.msToDataFormat(cache.getBucketKey().get(0)));

        //2024-03-06T17:11:04.056 + 500 => 11:04.556 + 1000 => 11:05.556 => 11:05.000
        cache.add(1, "Hello world", curTimeMs + 500, 1000);
        Assertions.assertEquals("2024-03-06T17:11:05.000", Util.msToDataFormat(cache.getBucketKey().get(0)));
        //Проверяем что корзина не добавилась
        Assertions.assertEquals("[1709734265000]", cache.getBucketKey().toString());

        for (int i = 2; i < 10; i++) {
            cache.add(i, "Hello world", curTimeMs + (500 * i), 1000);
        }

        Assertions.assertEquals("[1709734265000, 1709734266000, 1709734267000, 1709734268000, 1709734269000]", cache.getBucketKey().toString());

        Statistic statistics = cache.flushAndGetStatistic(null, null, null).get(0);
        Assertions.assertEquals("{MapSize=10, BucketSize=5}", statistics.getFields().toString());

        for (int i = 10; i < 100; i++) {
            cache.add(i, "Hello world", curTimeMs + (500 * i), 1000);
        }

        statistics = cache.flushAndGetStatistic(null, null, null).get(0);
        Assertions.assertEquals("{MapSize=100, BucketSize=50}", statistics.getFields().toString());

        Cache.KeepAliveResult keepAliveResult = cache.keepAlive(threadEnvelope, curTimeMs);
        // Никаких обработок пачек не должно быть, так как
        Assertions.assertEquals("[]", keepAliveResult.getReadBucket().toString());
        Assertions.assertEquals(0, keepAliveResult.getCountRemove().get());

        statistics = cache.flushAndGetStatistic(null, null, null).get(0);
        Assertions.assertEquals("{MapSize=100, BucketSize=50}", statistics.getFields().toString());

        keepAliveResult = cache.keepAlive(threadEnvelope, curTimeMs + 100);
        Assertions.assertEquals(0, keepAliveResult.getCountRemove().get());
        Assertions.assertEquals("[]", keepAliveResult.getReadBucket().toString());
        statistics = cache.flushAndGetStatistic(null, null, null).get(0);
        Assertions.assertEquals("{MapSize=100, BucketSize=50}", statistics.getFields().toString());

        Assertions.assertEquals("2024-03-06T17:11:05.006", Util.msToDataFormat(curTimeMs + 950));
        Assertions.assertEquals("2024-03-06T17:11:05.000", Util.msToDataFormat(Util.zeroLastNDigits(curTimeMs + 950, 3)));

        keepAliveResult = cache.keepAlive(threadEnvelope, curTimeMs + 950);
        Assertions.assertEquals(2, keepAliveResult.getCountRemove().get());
        Assertions.assertEquals("[2024-03-06T17:11:05.000]", keepAliveResult.getReadBucketFormat().toString());
        statistics = cache.flushAndGetStatistic(null, null, null).get(0);
        Assertions.assertEquals("{MapSize=98, BucketSize=49}", statistics.getFields().toString());

        keepAliveResult = cache.keepAlive(threadEnvelope, curTimeMs + (500 * 10));
        // 8 потому что 2 уже были удалены до этого
        Assertions.assertEquals(8, keepAliveResult.getCountRemove().get());
        // Это значит пробежка была от 2024-03-06T17:11:05.000 до 2024-03-06T17:11:05.999

        Assertions.assertEquals("[2024-03-06T17:11:06.000, 2024-03-06T17:11:07.000, 2024-03-06T17:11:08.000, 2024-03-06T17:11:09.000]", keepAliveResult.getReadBucketFormat().toString());
        statistics = cache.flushAndGetStatistic(null, null, null).get(0);
        Assertions.assertEquals("{MapSize=90, BucketSize=45}", statistics.getFields().toString());

    }

    Map<String, Object> multiThread(int sleepKeepAlive, int timeoutMs) {
        Cache<Integer, String> cache = new Cache<>();
        AvgMetric avgMetric = new AvgMetric();
        cache.setOnExpired((TimeEnvelope<String> env) -> {
            if (env.getExpiryRemainingMs() > 0) {
                Assertions.fail("ALARMA");
            } else {
                avgMetric.add(env.getExpiryRemainingMs() * -1);
            }
        });
        AtomicBoolean isRun = new AtomicBoolean(true);
        AtomicBoolean isRun2 = new AtomicBoolean(true);
        AtomicInteger counter = new AtomicInteger(0);

        ThreadEnvelope te2 = TestThreadEnvelope.get();

        //Сначала надо запустить keepAlive потому что старт потоков будет медленный и мы начнём терять секунды так как не запущенны
        new Thread(() -> {
            while (isRun2.get()) {
                long cur = System.currentTimeMillis();
                Cache.KeepAliveResult keepAliveResult = cache.keepAlive(te2, cur);
                System.out.println(keepAliveResult);
                Util.sleepMs(sleepKeepAlive);
            }
        }).start();

        for (int i = 0; i < 4; i++) {
            final int x = i;
            Util.sleepMs(333 * i); // Сделаем рассинхрон вставок по времени
            new Thread(() -> {
                while (isRun.get()) {
                    for (int j = 0; j < 1000; j++) {
                        try {
                            cache.add(counter.getAndIncrement(), java.util.UUID.randomUUID().toString(), timeoutMs);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Util.sleepMs((100 * x) + 10);
                }
            }).start();
        }


        Util.sleepMs(5000);
        isRun.set(false);
        Util.sleepMs(timeoutMs + sleepKeepAlive);
        isRun2.set(false);
        Map<String, Object> flush = avgMetric.flush("");
        Map<String, Object> fields = cache.flushAndGetStatistic(null, null, null).get(0).getFields();
        flush.putAll(fields);
        System.out.println(flush);
        Assertions.assertEquals(0, flush.get("MapSize"));
        Assertions.assertEquals(0, flush.get("BucketSize"));
        return flush;
    }

    @Test
    void singleRunThreadAny() {

    }

    @Test
    void runThreadAny() {
        //{AvgCount=5357983, Min=-276, Max=-1, Sum=-449148926, Avg=-83.82798638965447, MapSize=0, BucketSize=0}

        Map<String, Object> stat = multiThread(100, 500);
        double timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 100);

        stat = multiThread(100, 1000);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 100);

        stat = multiThread(200, 600);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 200);

        stat = multiThread(600, 600);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 600);

        stat = multiThread(1200, 600);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 1200);

        stat = multiThread(2400, 600);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 2400);

        stat = multiThread(4800, 600);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 4800);

    }

}