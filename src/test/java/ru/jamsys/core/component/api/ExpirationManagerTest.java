package ru.jamsys.core.component.api;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.component.manager.ExpirationManager;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.util.ExpirationKeepAliveResult;
import ru.jamsys.core.util.Util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class ExpirationManagerTest {
    AtomicBoolean isThreadRun = new AtomicBoolean(true);

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    public static class XItem {

    }

    @Test
    void testStop() {
        long curTimeMs = 1709734264056L; //2024-03-06T17:11:04.056
        @SuppressWarnings("unchecked")
        ExpirationManager<XItem> expirationManager = App.context.getBean(ExpirationManager.class);
        Expiration<XItem> test = expirationManager.get("test");
        AtomicInteger counterExpired = new AtomicInteger(0);
        test.setOnExpired(_ -> counterExpired.incrementAndGet());
        ExpirationMsImmutableEnvelope<XItem> add = test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs));
        //Стопаем задачу, что не выполнилсмя onExpired
        add.stop();
        ExpirationKeepAliveResult keepAliveResult = test.keepAlive(isThreadRun, curTimeMs + 1001);
        Assertions.assertEquals(1, keepAliveResult.getCountRemove().get());

        Assertions.assertEquals(0, counterExpired.get());

        Statistic statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=0, BucketSize=0}", statistics.getFields().toString());

    }

    @Test
    void checkSize() {
        long curTimeMs = 1709734264056L; //2024-03-06T17:11:04.056
        @SuppressWarnings("unchecked")
        ExpirationManager<XItem> expirationManager = App.context.getBean(ExpirationManager.class);
        Expiration<XItem> test = expirationManager.get("test");
        test.setOnExpired(System.out::println);
        test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs));


        //Проверяем что пока 1 корзина
        Assertions.assertEquals("[1709734265000]", test.getBucketKey().toString());
        Assertions.assertEquals("2024-03-06T17:11:05.000", Util.msToDataFormat(test.getBucketKey().getFirst()));

        //2024-03-06T17:11:04.056 + 500 => 11:04.556 + 1000 => 11:05.556 => 11:05.000

        test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs + 500));
        Assertions.assertEquals("2024-03-06T17:11:05.000", Util.msToDataFormat(test.getBucketKey().getFirst()));
        //Проверяем что корзина не добавилась
        Assertions.assertEquals("[1709734265000]", test.getBucketKey().toString());
//
        for (int i = 2; i < 10; i++) {
            test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs + (500 * i)));
        }

        Assertions.assertEquals("[1709734265000, 1709734266000, 1709734267000, 1709734268000, 1709734269000]", test.getBucketKey().toString());

        Statistic statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=10, BucketSize=5}", statistics.getFields().toString());

        for (int i = 10; i < 100; i++) {
            test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs + (500 * i)));
        }

        statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=100, BucketSize=50}", statistics.getFields().toString());

        ExpirationKeepAliveResult keepAliveResult = test.keepAlive(isThreadRun, curTimeMs);
        // Никаких обработок пачек не должно быть, так как
        Assertions.assertEquals("[]", keepAliveResult.getReadBucket().toString());
        Assertions.assertEquals(0, keepAliveResult.getCountRemove().get());

        statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=100, BucketSize=50}", statistics.getFields().toString());

        keepAliveResult = test.keepAlive(isThreadRun, curTimeMs + 100);
        Assertions.assertEquals(0, keepAliveResult.getCountRemove().get());
        Assertions.assertEquals("[]", keepAliveResult.getReadBucket().toString());
        statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=100, BucketSize=50}", statistics.getFields().toString());

        Assertions.assertEquals("2024-03-06T17:11:05.006", Util.msToDataFormat(curTimeMs + 950));
        Assertions.assertEquals("2024-03-06T17:11:05.000", Util.msToDataFormat(Util.zeroLastNDigits(curTimeMs + 950, 3)));

        keepAliveResult = test.keepAlive(isThreadRun, curTimeMs + 950);
        Assertions.assertEquals(2, keepAliveResult.getCountRemove().get());
        Assertions.assertEquals("[2024-03-06T17:11:05.000]", keepAliveResult.getReadBucketFormat().toString());
        statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=98, BucketSize=49}", statistics.getFields().toString());

        keepAliveResult = test.keepAlive(isThreadRun, curTimeMs + (500 * 10));
        // 8 потому что 2 уже были удалены до этого
        Assertions.assertEquals(8, keepAliveResult.getCountRemove().get());
        // Это значит пробежка была от 2024-03-06T17:11:05.000 до 2024-03-06T17:11:05.999

        Assertions.assertEquals("[2024-03-06T17:11:06.000, 2024-03-06T17:11:07.000, 2024-03-06T17:11:08.000, 2024-03-06T17:11:09.000]", keepAliveResult.getReadBucketFormat().toString());
        statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=90, BucketSize=45}", statistics.getFields().toString());

    }


    Map<String, Object> multiThread(int sleepKeepAlive, int timeoutMs) {
        @SuppressWarnings("unchecked")
        ExpirationManager<XItem> expirationManager = App.context.getBean(ExpirationManager.class);
        Expiration<XItem> test = expirationManager.get("test");

        AvgMetric avgMetric = new AvgMetric();
        test.setOnExpired((ExpirationMsImmutableEnvelope<XItem> env) -> {
            if (env.getExpiryRemainingMs() > 0) {
                Assertions.fail("ALARM");
            } else {
                avgMetric.add(env.getExpiryRemainingMs() * -1);
            }
        });
        AtomicBoolean isRun = new AtomicBoolean(true);
        AtomicBoolean isRun2 = new AtomicBoolean(true);

        //Сначала надо запустить keepAlive потому что старт потоков будет медленный и мы начнём терять секунды так как не запущенны
        new Thread(() -> {
            while (isRun2.get()) {
                long cur = System.currentTimeMillis();
                ExpirationKeepAliveResult keepAliveResult = test.keepAlive(isThreadRun, cur);
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
                            test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), timeoutMs));
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
        Map<String, Object> fields = test.flushAndGetStatistic(null, null, null).getFirst().getFields();
        flush.putAll(fields);
        System.out.println(flush);
        Assertions.assertEquals(0, flush.get("ItemSize"));
        Assertions.assertEquals(0, flush.get("BucketSize"));
        return flush;
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