package ru.jamsys.rate.limit.v2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
    void test() {
        long curTime = 1709734264056L; //2024-03-06T17:11:04.056
        long aLong = 60_000L;

        RateLimitItemPeriodic rateLimitItemPeriodic = new RateLimitItemPeriodic(Unit.MINUTE);
        Assertions.assertEquals("{period=Minute, max=-1, nextTime=2024-03-06T17:12:04.056, tpv=0, flushed=true}", rateLimitItemPeriodic.flushTps(curTime).toString());
        rateLimitItemPeriodic.check(null);
        Assertions.assertEquals("{period=Minute, max=-1, nextTime=2024-03-06T17:12:04.056, tpv=1, flushed=false}", rateLimitItemPeriodic.flushTps(curTime).toString());
        Assertions.assertEquals("{period=Minute, max=-1, nextTime=2024-03-06T17:13:04.056, tpv=0, flushed=true}", rateLimitItemPeriodic.flushTps(curTime + 60_000).toString());

        rateLimitItemPeriodic = new RateLimitItemPeriodic(Unit.HOUR_OF_DAY);
        Assertions.assertEquals("{period=HourOfDay, max=-1, nextTime=2024-03-06T18:11:04.056, tpv=0, flushed=true}", rateLimitItemPeriodic.flushTps(curTime).toString());

        rateLimitItemPeriodic = new RateLimitItemPeriodic(Unit.DAY_OF_WEEK);
        Assertions.assertEquals("{period=DayOfWeek, max=-1, nextTime=2024-03-07T17:11:04.056, tpv=0, flushed=true}", rateLimitItemPeriodic.flushTps(curTime).toString());

        rateLimitItemPeriodic = new RateLimitItemPeriodic(Unit.MONTH);
        Assertions.assertEquals("{period=Month, max=-1, nextTime=2024-04-06T17:11:04.056, tpv=0, flushed=true}", rateLimitItemPeriodic.flushTps(curTime).toString());
        rateLimitItemPeriodic.check(null);
        Assertions.assertEquals("{period=Month, max=-1, nextTime=2024-04-06T17:11:04.056, tpv=1, flushed=false}", rateLimitItemPeriodic.flushTps(curTime).toString());
        rateLimitItemPeriodic.check(null);
        Assertions.assertEquals("{period=Month, max=-1, nextTime=2024-04-06T17:11:04.056, tpv=2, flushed=false}", rateLimitItemPeriodic.flushTps(curTime).toString());
        rateLimitItemPeriodic.check(null);

        Assertions.assertEquals("2024-03-06T17:12:04.056", Util.msToDataFormat(curTime + (aLong)));
        Assertions.assertEquals("2024-03-06T18:11:04.056", Util.msToDataFormat(curTime + (aLong * 60)));
        Assertions.assertEquals("2024-03-07T17:11:04.056", Util.msToDataFormat(curTime + (aLong * 60 * 24)));
        Assertions.assertEquals("2024-04-07T17:11:04.056", Util.msToDataFormat(curTime + (aLong * 60 * 24 * 32)));

        Assertions.assertEquals("{period=Month, max=-1, nextTime=2024-04-06T17:11:04.056, tpv=3, flushed=false}", rateLimitItemPeriodic.flushTps(curTime).toString());
        Assertions.assertEquals("{period=Month, max=-1, nextTime=2024-04-06T17:11:04.056, tpv=3, flushed=false}", rateLimitItemPeriodic.flushTps(curTime + aLong).toString());
        Assertions.assertEquals("{period=Month, max=-1, nextTime=2024-04-06T17:11:04.056, tpv=3, flushed=false}", rateLimitItemPeriodic.flushTps(curTime + (aLong * 60)).toString());
        Assertions.assertEquals("{period=Month, max=-1, nextTime=2024-04-06T17:11:04.056, tpv=3, flushed=false}", rateLimitItemPeriodic.flushTps(curTime + (aLong * 60 * 24)).toString());
        Assertions.assertEquals("{period=Month, max=-1, nextTime=2024-04-06T17:11:04.056, tpv=3, flushed=false}", rateLimitItemPeriodic.flushTps(curTime + (aLong * 60 * 24 * 30)).toString());
        Assertions.assertEquals("{period=Month, max=-1, nextTime=2024-05-07T17:11:04.056, tpv=0, flushed=true}", rateLimitItemPeriodic.flushTps(curTime + (aLong * 60 * 24 * 32)).toString());

    }
}