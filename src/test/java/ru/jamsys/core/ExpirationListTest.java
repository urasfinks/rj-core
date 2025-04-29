package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.ExpirationList;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// IO time: 1min 3 sec
// COMPUTE time: 1min 3 sec

class ExpirationListTest {

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
        ExpirationList<XItem> test = App.get(Manager.class).configure(
                ExpirationList.class,
                "test",
                s -> new ExpirationList<>(s, _ -> counterExpired.incrementAndGet())
        ).getGeneric();


        ExpirationMsImmutableEnvelope<XItem> add = test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs));
        //Останавливаем задачу, что не выполнился onExpired
        add.stop();
        List<DataHeader> dataHeadersBefore = test.flushAndGetStatistic(threadRun);
        test.helper(threadRun, curTimeMs + 1001);
        List<DataHeader> dataHeadersAfter = test.flushAndGetStatistic(threadRun);
        Assertions.assertEquals(dataHeadersBefore, dataHeadersAfter);

        Assertions.assertEquals(0, counterExpired.get());

        DataHeader statistics = test.flushAndGetStatistic(null).getFirst();
        Assertions.assertEquals("{ItemSize=0, BucketSize=0}", statistics.getHeader().toString());
    }

    @Test
    void checkSize() {
        long curTimeMs = 1709734264056L; //2024-03-06T17:11:04.056
        ExpirationList<XItem> test = App.get(Manager.class).configure(
                ExpirationList.class,
                "test",
                s -> new ExpirationList<>(
                        s,
                        _ -> {}
                )
        ).getGeneric();
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

        DataHeader statistics = test.flushAndGetStatistic(null).getFirst();
        Assertions.assertEquals("{ItemSize=10, BucketSize=5}", statistics.getHeader().toString());

        for (int i = 10; i < 100; i++) {
            test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs + (500 * i)));
        }

        List<DataHeader> before = test.flushAndGetStatistic(null);
        Assertions.assertEquals("{ItemSize=100, BucketSize=50}", before.getFirst().getHeader().toString());

        test.helper(threadRun, curTimeMs);
        List<DataHeader> after = test.flushAndGetStatistic(null);
        Assertions.assertEquals(before.getFirst().getHeader().toString(), after.getFirst().getHeader().toString());

        before = test.flushAndGetStatistic(null);
        Assertions.assertEquals("{ItemSize=100, BucketSize=50}", before.getFirst().getHeader().toString());

        test.helper(threadRun, curTimeMs + 100);
        after = test.flushAndGetStatistic(null);
        Assertions.assertEquals(before.getFirst().getHeader().toString(), after.getFirst().getHeader().toString());
        statistics = test.flushAndGetStatistic(null).getFirst();
        Assertions.assertEquals("{ItemSize=100, BucketSize=50}", statistics.getHeader().toString());

        Assertions.assertEquals("2024-03-06T17:11:05.006", UtilDate.msFormat(curTimeMs + 950));
        Assertions.assertEquals("2024-03-06T17:11:05.000", UtilDate.msFormat(Util.zeroLastNDigits(curTimeMs + 950, 3)));

        test.helper(threadRun, curTimeMs + 950);

//        Assertions.assertEquals(2, keepAliveResult.getCountRemove().get());
//        Assertions.assertEquals("[2024-03-06T17:11:05.000]", keepAliveResult.getReadBucketFormat().toString());
//        statistics = test.flushAndGetStatistic(null, null, null).getFirst();
//        Assertions.assertEquals("{ItemSize=98, BucketSize=49}", statistics.getFields().toString());
//
//        keepAliveResult = test.helper(threadRun, curTimeMs + (500 * 10));
//        // 8 потому что 2 уже были удалены до этого
//        Assertions.assertEquals(8, keepAliveResult.getCountRemove().get());
//        // Это значит пробежка была от 2024-03-06T17:11:05.000 до 2024-03-06T17:11:05.999
//
//        Assertions.assertEquals("[2024-03-06T17:11:06.000, 2024-03-06T17:11:07.000, 2024-03-06T17:11:08.000, 2024-03-06T17:11:09.000]", keepAliveResult.getReadBucketFormat().toString());
//        statistics = test.flushAndGetStatistic(null, null, null).getFirst();
//        Assertions.assertEquals("{ItemSize=90, BucketSize=45}", statistics.getFields().toString());

    }

    Map<String, Object> multiThread(int sleepKeepAlive, int timeoutMs) {
        AvgMetric avgMetric = new AvgMetric();
        ExpirationList<XItem> test = App.get(Manager.class).configure(
                ExpirationList.class,
                "test",
                s -> new ExpirationList<>(s, env -> {
                    if (env.getExpiryRemainingMs() > 0) {
                        Assertions.fail("ALARM");
                    } else {
                        avgMetric.add(env.getExpiryRemainingMs() * -1);
                    }
                })
        ).getGeneric();

        AtomicBoolean run = new AtomicBoolean(true);
        AtomicBoolean run2 = new AtomicBoolean(true);

        //Сначала надо запустить keepAlive потому что старт потоков будет медленный и мы начнём терять секунды так как не запущенны

        Thread ka = new Thread(() -> {
            Thread.currentThread().setName("TMP KeepAlive");
            while (run2.get()) {
                try {
                    long cur = System.currentTimeMillis();
                    test.helper(threadRun, cur);
                    Util.testSleepMs(sleepKeepAlive);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        ka.start();

        for (int i = 0; i < 4; i++) {
            final int x = i;
            Util.testSleepMs(333 * i); // Сделаем разбивку вставок по времени
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
        Map<String, Object> fields = test.flushAndGetStatistic(null).getFirst().getHeader();
        flush.putAll(fields);
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