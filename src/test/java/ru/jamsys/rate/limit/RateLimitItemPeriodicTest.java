package ru.jamsys.rate.limit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.rate.limit.item.RateLimitItem;
import ru.jamsys.rate.limit.item.RateLimitItemMax;
import ru.jamsys.rate.limit.item.RateLimitItemPeriodic;
import ru.jamsys.rate.limit.item.RateLimitItemTps;
import ru.jamsys.template.cron.Unit;
import ru.jamsys.util.Util;

class RateLimitItemPeriodicTest {

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

        RateLimitItemPeriodic rateLimitItemPeriodic = new RateLimitItemPeriodic(Unit.MINUTE);
        Assertions.assertEquals("{period=Minute, max=-1, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-06T17:12:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check(null);
        // Тут не должно произойти сброс tpu - так как время сброса 2024-03-06T17:12:04.056
        Assertions.assertEquals("{period=Minute, max=-1, tpu=1, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-06T17:12:04.056", rateLimitItemPeriodic.getNextTime());
        // При сбросе tpu возвращается предыдущее значение для статистики, это принцип работы flush
        Assertions.assertEquals("{period=Minute, max=-1, tpu=1, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + 60_000, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-06T17:13:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Minute, max=-1, tpu=0, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + 60_000, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-06T17:13:04.056", rateLimitItemPeriodic.getNextTime());

        rateLimitItemPeriodic = new RateLimitItemPeriodic(Unit.HOUR_OF_DAY);
        Assertions.assertEquals("{period=HourOfDay, max=-1, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-06T18:11:04.056", rateLimitItemPeriodic.getNextTime());

        rateLimitItemPeriodic = new RateLimitItemPeriodic(Unit.DAY_OF_WEEK);
        Assertions.assertEquals("{period=DayOfWeek, max=-1, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-03-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());

        rateLimitItemPeriodic = new RateLimitItemPeriodic(Unit.MONTH);
        Assertions.assertEquals("{period=Month, max=-1, tpu=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check(null);
        Assertions.assertEquals("{period=Month, max=-1, tpu=1, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check(null);
        Assertions.assertEquals("{period=Month, max=-1, tpu=2, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check(null);

        Assertions.assertEquals("2024-03-06T17:12:04.056", Util.msToDataFormat(curTime + (aLong)));
        Assertions.assertEquals("2024-03-06T18:11:04.056", Util.msToDataFormat(curTime + (aLong * 60)));
        Assertions.assertEquals("2024-03-07T17:11:04.056", Util.msToDataFormat(curTime + (aLong * 60 * 24)));
        Assertions.assertEquals("2024-04-07T17:11:04.056", Util.msToDataFormat(curTime + (aLong * 60 * 24 * 32)));

        Assertions.assertEquals("{period=Month, max=-1, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime, null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=-1, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + aLong, null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=-1, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60), null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=-1, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24), null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=-1, tpu=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 30), null, null).getFields().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=-1, tpu=3, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 32), null, null).getFields().toString());
        Assertions.assertEquals("2024-05-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=-1, tpu=0, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 32), null, null).getFields().toString());
        Assertions.assertEquals("2024-05-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());
    }

    @Test
    void checkMax() {
        RateLimitItem rateLimitMax = new RateLimitItemMax();
        rateLimitMax.setMax(2L);
        Assertions.assertTrue(rateLimitMax.check(1));
        Assertions.assertTrue(rateLimitMax.check(2));
        Assertions.assertFalse(rateLimitMax.check(3));
        rateLimitMax.setMax(-1L);
        Assertions.assertTrue(rateLimitMax.check(3));
        Assertions.assertTrue(rateLimitMax.check(3));
        // Эту функцию будут вызывать при установки новых значений, низя
        Assertions.assertTrue(rateLimitMax.check(-1));
        Assertions.assertFalse(rateLimitMax.check(-2));
        Assertions.assertFalse(rateLimitMax.check(-3));
    }

    @Test
    void checkTps() {
        RateLimitItem rateLimitTps = new RateLimitItemTps();
        rateLimitTps.setMax(2L);
        Assertions.assertTrue(rateLimitTps.check(null));
        Assertions.assertTrue(rateLimitTps.check(null));
        Assertions.assertFalse(rateLimitTps.check(null));
    }
}