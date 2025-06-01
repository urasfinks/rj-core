package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.rate.limit.periodic.RateLimitPeriodic;
import ru.jamsys.core.extension.rate.limit.tps.RateLimitTps;
import ru.jamsys.core.flat.util.UtilDate;


// IO time: 6ms
// COMPUTE time: 5ms

class RateLimitTest {

    @BeforeAll
    static void beforeAll() {
        // runSpring что бы Manager не делал flushAndGetStatistic
        App.getRunBuilder().addTestArguments().runSpring();
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

        App.get(ServiceProperty.class).set("$.RateLimitPeriodic.min.period", "MINUTE");
        App.get(ServiceProperty.class).set("$.RateLimitPeriodic.hour.period", "HOUR_OF_DAY");
        App.get(ServiceProperty.class).set("$.RateLimitPeriodic.hour.period", "HOUR_OF_DAY");
        App.get(ServiceProperty.class).set("$.RateLimitPeriodic.day.period", "DAY_OF_WEEK");
        App.get(ServiceProperty.class).set("$.RateLimitPeriodic.month.period", "MONTH");

        ManagerConfiguration<RateLimitPeriodic> minConfigure = ManagerConfiguration.getInstance(RateLimitPeriodic.class, java.util.UUID.randomUUID().toString(), "min", null);
        RateLimitPeriodic rateLimitItemPeriodic = minConfigure.get();


        rateLimitItemPeriodic.setMax(999999);
        Assertions.assertEquals("{period=Minute, max=999999, tpp=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-03-06T17:12:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check();
        // Тут не должно произойти сброс tpu - так как время сброса 2024-03-06T17:12:04.056
        Assertions.assertEquals("{period=Minute, max=999999, tpp=1, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-03-06T17:12:04.056", rateLimitItemPeriodic.getNextTime());
        // При сбросе tpu возвращается предыдущее значение для статистики, это принцип работы flush
        Assertions.assertEquals("{period=Minute, max=999999, tpp=1, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + 60_000).getHeader().toString());
        Assertions.assertEquals("2024-03-06T17:13:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Minute, max=999999, tpp=0, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + 60_000).getHeader().toString());
        Assertions.assertEquals("2024-03-06T17:13:04.056", rateLimitItemPeriodic.getNextTime());


        ManagerConfiguration<RateLimitPeriodic> hourConfigure = ManagerConfiguration.getInstance(RateLimitPeriodic.class,java.util.UUID.randomUUID().toString(), "hour",null);
        rateLimitItemPeriodic = hourConfigure.get();

        rateLimitItemPeriodic.setMax(999999);
        Assertions.assertEquals("{period=HourOfDay, max=999999, tpp=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-03-06T18:11:04.056", rateLimitItemPeriodic.getNextTime());

        ManagerConfiguration<RateLimitPeriodic> dayConfigure = ManagerConfiguration.getInstance(RateLimitPeriodic.class,java.util.UUID.randomUUID().toString(), "day", null);
        rateLimitItemPeriodic = dayConfigure.get();

        rateLimitItemPeriodic.setMax(999999);
        Assertions.assertEquals("{period=DayOfWeek, max=999999, tpp=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-03-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());

        ManagerConfiguration<RateLimitPeriodic> monthConfigure = ManagerConfiguration.getInstance(RateLimitPeriodic.class,java.util.UUID.randomUUID().toString(), "month", null);
        rateLimitItemPeriodic = monthConfigure.get();

        rateLimitItemPeriodic.setMax(999999);
        Assertions.assertEquals("{period=Month, max=999999, tpp=0, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check();
        Assertions.assertEquals("{period=Month, max=999999, tpp=1, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check();
        Assertions.assertEquals("{period=Month, max=999999, tpp=2, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        rateLimitItemPeriodic.check();

        Assertions.assertEquals("2024-03-06T17:12:04.056", UtilDate.msFormat(curTime + (aLong)));
        Assertions.assertEquals("2024-03-06T18:11:04.056", UtilDate.msFormat(curTime + (aLong * 60)));
        Assertions.assertEquals("2024-03-07T17:11:04.056", UtilDate.msFormat(curTime + (aLong * 60 * 24)));
        Assertions.assertEquals("2024-04-07T17:11:04.056", UtilDate.msFormat(curTime + (aLong * 60 * 24 * 32)));

        //RateLimitTest.testPeriodic:100 expected: <{period=Month, max=999999, tpp=3, flushed=false}> but was: <{period=Month, max=999999, tpp=0, flushed=false}>
        //[ERROR]
        Assertions.assertEquals("{period=Month, max=999999, tpp=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpp=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + aLong).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpp=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60)).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpp=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24)).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpp=3, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 30)).getHeader().toString());
        Assertions.assertEquals("2024-04-06T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpp=3, flushed=true}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 32)).getHeader().toString());
        Assertions.assertEquals("2024-05-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());
        Assertions.assertEquals("{period=Month, max=999999, tpp=0, flushed=false}", rateLimitItemPeriodic.flushAndGetStatistic(curTime + (aLong * 60 * 24 * 32)).getHeader().toString());
        Assertions.assertEquals("2024-05-07T17:11:04.056", rateLimitItemPeriodic.getNextTime());
    }

    @Test
    void testTps() {
        ManagerConfiguration<RateLimitTps> tpsConfigure = ManagerConfiguration.getInstance(RateLimitTps.class, java.util.UUID.randomUUID().toString(),"tps", null);
        RateLimitTps rateLimitTps = tpsConfigure.get();
        rateLimitTps.run();
        rateLimitTps.setMax(2);
        Assertions.assertTrue(rateLimitTps.check());
        Assertions.assertTrue(rateLimitTps.check());
        Assertions.assertFalse(rateLimitTps.check());
    }

}