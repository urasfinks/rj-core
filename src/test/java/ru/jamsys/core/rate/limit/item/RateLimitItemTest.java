package ru.jamsys.core.rate.limit.item;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.template.cron.TimeUnit;
import ru.jamsys.core.flat.util.Util;

class RateLimitItemTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        //App.main(args); мы не можем стартануть проект, так как запустится keepAlive
        // который будет сбрасывать счётчики tps и тесты будут разваливаться
        App.main(args);
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

        RateLimitItemPeriodic rateLimitItemPeriodic = new RateLimitItemPeriodic(App.context, TimeUnit.MINUTE, "min");
        rateLimitItemPeriodic.set("max", 999999);
        Assertions.assertEquals("{period=Minute, max=999999, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-06T17:12:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check(null);
        // Тут не должно произойти сброс tpu - так как время сброса 2024-03-06T17:12:04.056
        Assertions.assertEquals("{period=Minute, max=999999, tpu=1, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-06T17:12:04.056", rateLimitItemPeriodic.getNextTime());
        // При сбросе tpu возвращается предыдущее значение для статистики, это принцип работы flush
        Assertions.assertEquals("{period=Minute, max=999999, tpu=1, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + 60_000, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-06T17:13:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Minute, max=999999, tpu=0, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + 60_000, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-06T17:13:04.056", rateLimitItemPeriodic.getNextTime());

        rateLimitItemPeriodic = new RateLimitItemPeriodic(App.context, TimeUnit.HOUR_OF_DAY, "hour");
        rateLimitItemPeriodic.set("max", 999999);
        Assertions.assertEquals("{period=HourOfDay, max=999999, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-06T18:11:04.056", rateLimitItemPeriodic.getNextTime());

        rateLimitItemPeriodic = new RateLimitItemPeriodic(App.context, TimeUnit.DAY_OF_WEEK, "day");
        rateLimitItemPeriodic.set("max", 999999);
        Assertions.assertEquals("{period=DayOfWeek, max=999999, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());

        rateLimitItemPeriodic = new RateLimitItemPeriodic(App.context, TimeUnit.MONTH, "month");
        rateLimitItemPeriodic.set("max", 999999);
        Assertions.assertEquals("{period=Month, max=999999, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check(null);
        Assertions.assertEquals("{period=Month, max=999999, tpu=1, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check(null);
        Assertions.assertEquals("{period=Month, max=999999, tpu=2, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check(null);

        Assertions.assertEquals("2024-03-06T17:12:04.056", Util.msToDataFormat(curTime + (aLong)));
        Assertions.assertEquals("2024-03-06T18:11:04.056", Util.msToDataFormat(curTime + (aLong * 60)));
        Assertions.assertEquals("2024-03-07T17:11:04.056", Util.msToDataFormat(curTime + (aLong * 60 * 24)));
        Assertions.assertEquals("2024-04-07T17:11:04.056", Util.msToDataFormat(curTime + (aLong * 60 * 24 * 32)));

        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + aLong, null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60), null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24), null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 30), null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=3, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 32), null, null).getFields().toString());
        Assertions.assertEquals("2024-05-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpu=0, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 32), null, null).getFields().toString());
        Assertions.assertEquals("2024-05-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());
    }

    @Test
    void testMax() {
        RateLimitItem rateLimitMax = new RateLimitItemMax(App.context, "min");
        rateLimitMax.set("max", 2);
        Assertions.assertTrue(rateLimitMax.check(1));
        Assertions.assertTrue(rateLimitMax.check(2));
        Assertions.assertFalse(rateLimitMax.check(3));
        rateLimitMax.set("max", -1);
        Assertions.assertFalse(rateLimitMax.check(3));
        Assertions.assertFalse(rateLimitMax.check(3));
        Assertions.assertTrue(rateLimitMax.check(-1));
        Assertions.assertTrue(rateLimitMax.check(-2));
        Assertions.assertTrue(rateLimitMax.check(-3));
    }

    @Test
    void testMin() {
        RateLimitItemMin rateLimitMin = new RateLimitItemMin(App.context, "min");
        rateLimitMin.set("min", 2);
        Assertions.assertFalse(rateLimitMin.check(1));
        Assertions.assertTrue(rateLimitMin.check(2));
        Assertions.assertTrue(rateLimitMin.check(3));
        rateLimitMin.set("min", -1);
        Assertions.assertTrue(rateLimitMin.check(3));
        Assertions.assertTrue(rateLimitMin.check(3));
        Assertions.assertTrue(rateLimitMin.check(-1));
        Assertions.assertFalse(rateLimitMin.check(-2));
        Assertions.assertFalse(rateLimitMin.check(-3));
    }

    @Test
    void testTps() {
        RateLimitItem rateLimitTps = new RateLimitItemTps(App.context, "tps");
        rateLimitTps.set("max", 2);
        Assertions.assertTrue(rateLimitTps.check(null));
        Assertions.assertTrue(rateLimitTps.check(null));
        Assertions.assertFalse(rateLimitTps.check(null));
    }

}