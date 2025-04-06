package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.component.manager.item.ExpirationKeepAliveResult;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// IO time: 1min 3 sec
// COMPUTE time: 1min 3 sec

class ExpirationTest {

    AtomicBoolean threadRun = new AtomicBoolean(true);

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runSpring();
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
        AtomicInteger counterExpired = new AtomicInteger(0);
        Expiration<XItem> test = App.get(ManagerExpiration.class).get("test", XItem.class, _ -> counterExpired.incrementAndGet());


        ExpirationMsImmutableEnvelope<XItem> add = test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs));
        //Стопаем задачу, что не выполнилсмя onExpired
        add.stop();
        ExpirationKeepAliveResult keepAliveResult = test.keepAlive(threadRun, curTimeMs + 1001);
        Assertions.assertEquals(1, keepAliveResult.getCountRemove().get());

        Assertions.assertEquals(0, counterExpired.get());

        Statistic statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=0, BucketSize=0}", statistics.getFields().toString());
    }

    @Test
    void checkSize() {
        long curTimeMs = 1709734264056L; //2024-03-06T17:11:04.056
        Expiration<XItem> test = App.get(ManagerExpiration.class).get("test", XItem.class, System.out::println);
        test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs));


        //Проверяем что пока 1 корзина
        Assertions.assertEquals("[1709734265000]", test.getBucketKey().toString());
        Assertions.assertEquals("2024-03-06T17:11:05.000", UtilDate.msFormat(test.getBucketKey().getFirst()));

        //2024-03-06T17:11:04.056 + 500 => 11:04.556 + 1000 => 11:05.556 => 11:05.000

        test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs + 500));
        Assertions.assertEquals("2024-03-06T17:11:05.000", UtilDate.msFormat(test.getBucketKey().getFirst()));
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

        ExpirationKeepAliveResult keepAliveResult = test.keepAlive(threadRun, curTimeMs);
        // Никаких обработок пачек не должно быть, так как
        Assertions.assertEquals("[]", keepAliveResult.getReadBucket().toString());
        Assertions.assertEquals(0, keepAliveResult.getCountRemove().get());

        statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=100, BucketSize=50}", statistics.getFields().toString());

        keepAliveResult = test.keepAlive(threadRun, curTimeMs + 100);
        Assertions.assertEquals(0, keepAliveResult.getCountRemove().get());
        Assertions.assertEquals("[]", keepAliveResult.getReadBucket().toString());
        statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=100, BucketSize=50}", statistics.getFields().toString());

        Assertions.assertEquals("2024-03-06T17:11:05.006", UtilDate.msFormat(curTimeMs + 950));
        Assertions.assertEquals("2024-03-06T17:11:05.000", UtilDate.msFormat(Util.zeroLastNDigits(curTimeMs + 950, 3)));

        keepAliveResult = test.keepAlive(threadRun, curTimeMs + 950);
        Assertions.assertEquals(2, keepAliveResult.getCountRemove().get());
        Assertions.assertEquals("[2024-03-06T17:11:05.000]", keepAliveResult.getReadBucketFormat().toString());
        statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=98, BucketSize=49}", statistics.getFields().toString());

        keepAliveResult = test.keepAlive(threadRun, curTimeMs + (500 * 10));
        // 8 потому что 2 уже были удалены до этого
        Assertions.assertEquals(8, keepAliveResult.getCountRemove().get());
        // Это значит пробежка была от 2024-03-06T17:11:05.000 до 2024-03-06T17:11:05.999

        Assertions.assertEquals("[2024-03-06T17:11:06.000, 2024-03-06T17:11:07.000, 2024-03-06T17:11:08.000, 2024-03-06T17:11:09.000]", keepAliveResult.getReadBucketFormat().toString());
        statistics = test.flushAndGetStatistic(null, null, null).getFirst();
        Assertions.assertEquals("{ItemSize=90, BucketSize=45}", statistics.getFields().toString());

    }

    Map<String, Object> multiThread(int sleepKeepAlive, int timeoutMs) {
        AvgMetric avgMetric = new AvgMetric();
        final Expiration<XItem> test = App.get(ManagerExpiration.class).get("test", XItem.class, (DisposableExpirationMsImmutableEnvelope<XItem> env) -> {
            if (env.getExpiryRemainingMs() > 0) {
                Assertions.fail("ALARM");
            } else {
                avgMetric.add(env.getExpiryRemainingMs() * -1);
            }
        });
        AtomicBoolean run = new AtomicBoolean(true);
        AtomicBoolean run2 = new AtomicBoolean(true);

        //Сначала надо запустить keepAlive потому что старт потоков будет медленный и мы начнём терять секунды так как не запущенны

        Thread ka = new Thread(() -> {
            Thread.currentThread().setName("TMP KeepAlive");
            while (run2.get()) {
                try {
                    long cur = System.currentTimeMillis();
                    ExpirationKeepAliveResult keepAliveResult = test.keepAlive(threadRun, cur);
                    System.out.println(keepAliveResult);
                    Util.testSleepMs(sleepKeepAlive);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        ka.start();

        for (int i = 0; i < 4; i++) {
            final int x = i;
            Util.testSleepMs(333 * i); // Сделаем рассинхрон вставок по времени
            new Thread(() -> {
                Thread.currentThread().setName("IOSIF " + x);
                try {
                    while (run.get()) {
                        for (int j = 0; j < 1000; j++) {
                            test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), timeoutMs));
                        }
                        Util.testSleepMs((100 * x) + 10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }


        Util.testSleepMs(5000);
        run.set(false);
        Util.testSleepMs(timeoutMs + sleepKeepAlive);
        run2.set(false);
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

        Map<String, Object> stat;
        double timeAvg;

        stat = multiThread(100, 500);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 100, timeAvg+"");

        stat = multiThread(100, 1000);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 100, timeAvg + "");

        stat = multiThread(200, 600);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 200, timeAvg+"");

        stat = multiThread(600, 600);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 600, timeAvg+"");

        stat = multiThread(1200, 600);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 1200, timeAvg+"");

        stat = multiThread(2400, 600);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 2400, timeAvg+"");

        stat = multiThread(4800, 600);
        timeAvg = (double) stat.get("Avg");
        Assertions.assertTrue(timeAvg <= 4800, timeAvg+"");

    }

}