package ru.jamsys.core;

import lombok.Getter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.component.manager.ManagerConfigurationFactory;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.expiration.ExpirationList;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// IO time:
// COMPUTE time:

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

    @Getter
    public static class XItem {
        private final int x = 0;
    }

    @Test
    void testStop() {
        long curTimeMs = 1709734264056L; //2024-03-06T17:11:04.056
        AtomicInteger counterExpired = new AtomicInteger(0);
        ManagerConfiguration<ExpirationList<XItem>> configureTest = ManagerConfigurationFactory.get(
                ExpirationList.class,
                "test1",
                xItemExpirationList -> xItemExpirationList.setupOnExpired(_ -> counterExpired.incrementAndGet())
        );
        ExpirationList<XItem> test = configureTest.get();

        ExpirationMsImmutableEnvelope<XItem> add = test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs));
        // 2024-03-06T17:11:04.056 + 1000 = 2024-03-06T17:11:05.056
        // resetLast3Digital (2024-03-06T17:11:05.000) = попал в корзину на удаление с ключом 2024-03-06T17:11:06.000

        //Останавливаем задачу, что бы не выполнился onExpired
        add.stop();
        List<DataHeader> s1 = test.flushAndGetStatistic(threadRun);
        Assertions.assertEquals("{ItemSize=1, BucketSize=1, helperRemove=0, helperOnExpired=0}", s1.getFirst().getHeader().toString());
        Assertions.assertEquals("[1709734266000]", test.getBucketKey().toString());
        test.helper(threadRun, curTimeMs + 2000);
        List<DataHeader> s2 = test.flushAndGetStatistic(threadRun);
        // helperRemove=1 так как выше выполнили add.stop();
        Assertions.assertEquals("{ItemSize=0, BucketSize=0, helperRemove=1, helperOnExpired=0}", s2.getFirst().getHeader().toString());

        Assertions.assertEquals(0, counterExpired.get());

        List<DataHeader> s3 = test.flushAndGetStatistic(null);
        Assertions.assertEquals("{ItemSize=0, BucketSize=0, helperRemove=0, helperOnExpired=0}", s3.getFirst().getHeader().toString());

        test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs));
        test.helper(threadRun, curTimeMs + 2000);
        List<DataHeader> s4 = test.flushAndGetStatistic(null);
        Assertions.assertEquals("{ItemSize=0, BucketSize=0, helperRemove=0, helperOnExpired=1}", s4.getFirst().getHeader().toString());

    }

    @Test
    void checkSize() {
        long curTimeMs = 1709734264056L; //2024-03-06T17:11:04.056
        ManagerConfiguration<ExpirationList<XItem>> testConfigure = ManagerConfigurationFactory.get(
                ExpirationList.class,
                "test2"
        );
        ExpirationList<XItem> test = testConfigure.get();

        test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs));


        //Проверяем что пока 1 корзина
        Assertions.assertEquals("[1709734266000]", test.getBucketKey().toString());
        Assertions.assertEquals("2024-03-06T17:11:06.000", UtilDate.msFormat(test.getBucketKey().getFirst()));

        //2024-03-06T17:11:04.056 + 500 => 11:04.556 + 1000 => 11:05.556 => 11:05.000

        test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs + 500));
        Assertions.assertEquals("2024-03-06T17:11:06.000", UtilDate.msFormat(test.getBucketKey().getFirst()));
        //Проверяем что корзина не добавилась
        Assertions.assertEquals("[1709734266000]", test.getBucketKey().toString());
//
        for (int i = 2; i < 10; i++) {
            test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs + (500 * i)));
        }

        Assertions.assertEquals("[1709734266000, 1709734267000, 1709734268000, 1709734269000, 1709734270000]", test.getBucketKey().toString());

        DataHeader statistics = test.flushAndGetStatistic(null).getFirst();
        Assertions.assertEquals("{ItemSize=10, BucketSize=5, helperRemove=0, helperOnExpired=0}", statistics.getHeader().toString());

        for (int i = 10; i < 100; i++) {
            test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1000, curTimeMs + (500 * i)));
        }

        List<DataHeader> before = test.flushAndGetStatistic(null);
        Assertions.assertEquals("{ItemSize=100, BucketSize=50, helperRemove=0, helperOnExpired=0}", before.getFirst().getHeader().toString());

        test.helper(threadRun, curTimeMs);
        List<DataHeader> after = test.flushAndGetStatistic(null);
        Assertions.assertEquals(before.getFirst().getHeader().toString(), after.getFirst().getHeader().toString());

        before = test.flushAndGetStatistic(null);
        Assertions.assertEquals("{ItemSize=100, BucketSize=50, helperRemove=0, helperOnExpired=0}", before.getFirst().getHeader().toString());

        test.helper(threadRun, curTimeMs + 100);
        after = test.flushAndGetStatistic(null);
        Assertions.assertEquals(before.getFirst().getHeader().toString(), after.getFirst().getHeader().toString());
        statistics = test.flushAndGetStatistic(null).getFirst();
        Assertions.assertEquals("{ItemSize=100, BucketSize=50, helperRemove=0, helperOnExpired=0}", statistics.getHeader().toString());

        Assertions.assertEquals("2024-03-06T17:11:05.006", UtilDate.msFormat(curTimeMs + 950));
        Assertions.assertEquals("2024-03-06T17:11:05.000", UtilDate.msFormat(Util.resetLastNDigits(curTimeMs + 950, 3)));

        test.helper(threadRun, curTimeMs + 950);

    }

    @Test
    public void multiThread() throws InterruptedException {
        AtomicInteger err = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);

        ManagerConfiguration<ExpirationList<XItem>> configureTest = ManagerConfigurationFactory.get(
                ExpirationList.class,
                "test3",
                xItemExpirationList -> xItemExpirationList.setupOnExpired(_ -> success.incrementAndGet())
        );
        ExpirationList<XItem> test = configureTest.get();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicBoolean run = new AtomicBoolean(true);
        long now = System.currentTimeMillis();
        long initialDelay = 1000 - (now % 1000);
        scheduler.scheduleAtFixedRate(
                () -> {
                    System.out.println(UtilDate.msFormat(System.currentTimeMillis()));
                    test.helper(run, System.currentTimeMillis());
                },
                initialDelay,
                1000,
                TimeUnit.MILLISECONDS
        );

        int c = 1_000_000;
        for (int i = 0; i < 4; i++) {
            Util.testSleepMs(250);
            Thread thread = new Thread(() -> {
                try {
                    long time = System.currentTimeMillis();
                    for (int j = 0; j < c; j++) {
                        test.add(new ExpirationMsImmutableEnvelope<>(new XItem(), 1333));
                    }
                    UtilLog.printInfo(System.currentTimeMillis() - time);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread.start();
            thread.join();
        }
        Util.testSleepMs(3_000);
        scheduler.shutdown();
        UtilLog.printInfo(test);

        Assertions.assertEquals(0, err.get());
        Assertions.assertEquals(4 * c, success.get());
        // Надо что бы среднее укладывалось в 1 секунду
    }

}