package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.template.cron.TimeUnit;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitPeriodic;
import ru.jamsys.core.rate.limit.RateLimitTps;

// IO time: 6ms
// COMPUTE time: 5ms

class RateLimitTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void singleTest() {
//        long curTime = 1709734264056L; //2024-03-06T17:11:04.056
//        long aLong = 60_000L;
//        RateLimitItemPeriodic rateLimitItemPeriodic;
    }

    @Test
    void testPeriodic() {
        long curTime = 1709734264056L; //2024-03-06T17:11:04.056
        long aLong = 60_000L;

        RateLimitPeriodic rateLimitItemPeriodic = (RateLimitPeriodic) RateLimitPeriodic.getInstanceConfigure("min", TimeUnit.MINUTE).get();


        rateLimitItemPeriodic.setMax(999999);
        Assertions.assertEquals("{period=Minute, max=999999, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-03-06T17:12:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check();
        // Тут не должно произойти сброс tpu - так как время сброса 2024-03-06T17:12:04.056
        Assertions.assertEquals("{period=Minute, max=999999, tpu=1, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-03-06T17:12:04.056", rateLimitItemPeriodic.getNextTime());
        // При сбросе tpu возвращается предыдущее значение для статистики, это принцип работы flush
        Assertions.assertEquals("{period=Minute, max=999999, tpu=1, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + 60_000).getHeader().toString());
        Assertions.assertEquals("2024-03-06T17:13:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Minute, max=999999, tpu=0, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + 60_000).getHeader().toString());
        Assertions.assertEquals("2024-03-06T17:13:04.056", rateLimitItemPeriodic.getNextTime());

        rateLimitItemPeriodic = (RateLimitPeriodic) RateLimitPeriodic.getInstanceConfigure("hour", TimeUnit.HOUR_OF_DAY).get();;
        rateLimitItemPeriodic.setMax(999999);
        Assertions.assertEquals("{period=HourOfDay, max=999999, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-03-06T18:11:04.056", rateLimitItemPeriodic.getNextTime());


        rateLimitItemPeriodic = (RateLimitPeriodic) RateLimitPeriodic.getInstanceConfigure("day", TimeUnit.DAY_OF_WEEK).get();
        rateLimitItemPeriodic.setMax(999999);
        Assertions.assertEquals("{period=DayOfWeek, max=999999, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-03-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());


        rateLimitItemPeriodic = (RateLimitPeriodic) RateLimitPeriodic.getInstanceConfigure("month", TimeUnit.MONTH).get();
        rateLimitItemPeriodic.setMax(999999);
        Assertions.assertEquals("{period=Month, max=999999, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check();
        Assertions.assertEquals("{period=Month, max=999999, tpu=1, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check();
        Assertions.assertEquals("{period=Month, max=999999, tpu=2, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check();

        Assertions.assertEquals("2024-03-06T17:12:04.056", UtilDate.msFormat(curTime + (aLong)));
        Assertions.assertEquals("2024-03-06T18:11:04.056", UtilDate.msFormat(curTime + (aLong * 60)));
        Assertions.assertEquals("2024-03-07T17:11:04.056", UtilDate.msFormat(curTime + (aLong * 60 * 24)));
        Assertions.assertEquals("2024-04-07T17:11:04.056", UtilDate.msFormat(curTime + (aLong * 60 * 24 * 32)));

        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + aLong).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60)).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24)).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 30)).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 32)).getHeader().toString());
        Assertions.assertEquals("2024-05-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=0, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 32)).getHeader().toString());
        Assertions.assertEquals("2024-05-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());
    }

    @Test
    void testTps() {
        RateLimit rateLimitTps = RateLimitTps.getInstanceConfigure("tps").get();
        rateLimitTps.run();
        rateLimitTps.setMax(2);
        Assertions.assertTrue(rateLimitTps.check());
        Assertions.assertTrue(rateLimitTps.check());
        Assertions.assertFalse(rateLimitTps.check());
    }

}