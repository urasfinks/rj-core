package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.util.UtilDate;

// IO time: 53
// COMPUTE time: 54

class CronTest {

    @Test
    public void single() {
        @SuppressWarnings("unused")
        long curTime = 1709734264056L; //2024-03-06T17:11:04.056
    }

    @Test
    public void test() {
        long curTime = 1709734264056L; //2024-03-06T17:11:04.056

        Assertions.assertEquals("Template({Second=[30], Minute=[8], HourOfDay=[10], DayOfMonth=[6], Month=[], DayOfWeek=[]})", new Cron("30 08 10 06 *").toString());
        Assertions.assertEquals("Template({Second=[1], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron("1 * * * *").toString());
        Assertions.assertEquals("Template({Second=[1, 2], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron("1-2 * * * *").toString());
        Assertions.assertEquals("Template({Second=[1, 2, 3], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron("1-3 * * * *").toString());
        Assertions.assertEquals("Template({Second=[0, 15, 30, 45], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron("*/15 * * * *").toString());
        Assertions.assertEquals("Template({Second=[1, 0, 15, 30, 45], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron("1,*/15 * * * *").toString());
        Assertions.assertEquals("Template({Second=[1, 2, 3, 0, 15, 30, 45], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron("1-3,*/15 * * * *").toString());
        Assertions.assertEquals("Template({Second=[0, 1, 2, 3, 15, 30, 45], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron("0-3,*/15 * * * *").toString());
        Assertions.assertEquals("Template({Second=[20, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34, 35], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron("20-25,30-35 * * * *").toString());
        Assertions.assertEquals("Template({Second=[], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron("60 * * * *").toString());
        Assertions.assertEquals("Template({Second=[59], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron("59,60,61 * * * *").toString());
        Assertions.assertEquals("Template({Second=[], Minute=[], HourOfDay=[1], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron("* * 1 * *").toString());
        Assertions.assertEquals("Template({Second=[], Minute=[], HourOfDay=[], DayOfMonth=[1], Month=[], DayOfWeek=[]})", new Cron("* * * 1 *").toString());
        Assertions.assertEquals("Template({Second=[], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[1], DayOfWeek=[]})", new Cron("* * * * 1").toString());
        Assertions.assertEquals("Template({Second=[], Minute=[], HourOfDay=[], DayOfMonth=[], Month=[], DayOfWeek=[1]})", new Cron("* * * * * 1").toString());
        Assertions.assertEquals("Template({Second=[0], Minute=[0], HourOfDay=[0], DayOfMonth=[1], Month=[], DayOfWeek=[]})", new Cron("0 0 0 1 *").toString());


        System.out.println("Start time: " + UtilDate.msFormat(curTime));
        Assertions.assertEquals("[{Second=1, Minute=null, HourOfDay=null, DayOfMonth=null, Month=null, DayOfWeek=null}]", new Cron("1 * * * *").getListTimeVariant().toString());

        //RateLimit test
        Assertions.assertEquals("2024-03-06T17:12:00.000", UtilDate.msFormat(new Cron("0 * * * *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T18:00:00.000", UtilDate.msFormat(new Cron("0 0 * * *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-07T00:00:00.000", UtilDate.msFormat(new Cron("0 0 0 * *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-04-01T00:00:00.000", UtilDate.msFormat(new Cron("0 0 0 1 *").compile(curTime).getNextTimestamp()));

        Assertions.assertNull(UtilDate.msFormat(new Cron("0 0 0 1 1").compile(curTime).getNextTimestamp()));

        Assertions.assertEquals("2024-03-06T17:12:01.000", UtilDate.msFormat(new Cron("1 * * * * *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T17:12:02.000", UtilDate.msFormat(new Cron("2 * * * * *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T17:11:05.000", UtilDate.msFormat(new Cron("* * * * * *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T17:11:05.000", UtilDate.msFormat(new Cron("*/1 * * * * *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T17:11:15.000", UtilDate.msFormat(new Cron("*/15 * * * * *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T18:00:00.000", UtilDate.msFormat(new Cron("* 0 * * * *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T18:01:00.000", UtilDate.msFormat(new Cron("* 1 * * * *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-07T01:00:00.000", UtilDate.msFormat(new Cron("* * 1 * * *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-04-01T00:00:00.000", UtilDate.msFormat(new Cron("* * * 1 * *").compile(curTime).getNextTimestamp()));
        //Assertions.assertEquals("2024-04-01T00:00:01", UtilDate.msToDataFormat(new Template("* * * 1 * *").getNext(1711918800000L)));
        Assertions.assertEquals("2024-03-06T17:11:05.000", UtilDate.msFormat(new Cron("* * * * 3 *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T17:11:15.000", UtilDate.msFormat(new Cron("15 * * * 3 *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T17:12:01.000", UtilDate.msFormat(new Cron("1 * * * 3 *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T17:13:01.000", UtilDate.msFormat(new Cron("1 13 * * 3 *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T18:10:01.000", UtilDate.msFormat(new Cron("1 10 * * 3 *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T18:10:01.000", UtilDate.msFormat(new Cron("1 10 18 * 3 *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-04-01T17:10:01.000", UtilDate.msFormat(new Cron("1 10 17 * 4 *").compile(curTime).getNextTimestamp()));
        Assertions.assertNull(UtilDate.msFormat(new Cron("1 10 17 15 4 *").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-11T00:00:00.000", UtilDate.msFormat(new Cron("* * * * * 1").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-12T00:00:00.000", UtilDate.msFormat(new Cron("* * * * * 2").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-06T17:11:05.000", UtilDate.msFormat(new Cron("* * * * * 3").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-07T00:00:00.000", UtilDate.msFormat(new Cron("* * * * * 4").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-08T00:00:00.000", UtilDate.msFormat(new Cron("* * * * * 5").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-09T00:00:00.000", UtilDate.msFormat(new Cron("* * * * * 6").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-10T00:00:00.000", UtilDate.msFormat(new Cron("* * * * * 7").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-10T00:00:01.000", UtilDate.msFormat(new Cron("1 * * * * 7").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-10T00:00:59.000", UtilDate.msFormat(new Cron("59 * * * * 7").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-10T00:00:19.000", UtilDate.msFormat(new Cron("19-20 * * * * 7").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-10T00:13:19.000", UtilDate.msFormat(new Cron("19-20 13 * * * 7").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-10T14:13:19.000", UtilDate.msFormat(new Cron("19-20 13 14 * * 7").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-10T14:13:19.000", UtilDate.msFormat(new Cron("19-20 13 14 10 * 7").compile(curTime).getNextTimestamp()));
        Assertions.assertEquals("2024-03-17T14:13:19.000", UtilDate.msFormat(new Cron("19-20 13 14 17 * 7").compile(curTime).getNextTimestamp()));
        Assertions.assertNull(UtilDate.msFormat(new Cron("19 13 14 17 3 7").compile(curTime).getNextTimestamp()));
        Assertions.assertNull(UtilDate.msFormat(new Cron("19 13 14 17 3 *").compile(curTime).getNextTimestamp()));


        Assertions.assertEquals("[2024-03-06T17:11:05.000, 2024-03-06T17:11:06.000]", new Cron("* * * * * *").getSeriesFormatted(curTime, 2).toString());
        Assertions.assertEquals("[2024-03-06T17:11:05.000, 2024-03-06T17:11:06.000, 2024-03-06T17:11:07.000]", new Cron("* * * * * *").getSeriesFormatted(curTime, 3).toString());
        Assertions.assertEquals("[2024-03-06T17:11:15.000, 2024-03-06T17:11:30.000]", new Cron("*/15 * * * * *").getSeriesFormatted(curTime, 2).toString());
        Assertions.assertEquals("[2024-03-06T18:15:15.000, 2024-03-07T18:15:15.000]", new Cron("15 15 18 * * *").getSeriesFormatted(curTime, 2).toString());
        Assertions.assertEquals("[2024-03-07T00:00:00.000, 2024-03-07T00:00:01.000]", new Cron("* * * 7 * *").getSeriesFormatted(curTime, 2).toString());
        Assertions.assertEquals("[2024-03-07T00:00:15.000, 2024-03-07T00:01:15.000]", new Cron("15 * * 7 * *").getSeriesFormatted(curTime, 2).toString());
        Assertions.assertEquals("[2024-03-07T00:15:15.000, 2024-03-07T01:15:15.000]", new Cron("15 15 * 7 * *").getSeriesFormatted(curTime, 2).toString());
        Assertions.assertEquals("[2024-03-07T00:15:15.000, 2024-03-07T01:15:15.000, 2024-03-07T02:15:15.000]", new Cron("15 15 * 7 * *").getSeriesFormatted(curTime, 3).toString());
        Assertions.assertEquals("[2024-03-07T15:15:15.000, 2024-04-07T15:15:15.000]", new Cron("15 15 15 7 * *").getSeriesFormatted(curTime, 2).toString());
        Assertions.assertEquals("[2024-03-07T15:15:15.000, 2024-03-08T15:15:15.000]", new Cron("15 15 15 * * *").getSeriesFormatted(curTime, 2).toString());
        Assertions.assertEquals("[2024-07-01T15:15:15.000, 2024-07-02T15:15:15.000]", new Cron("15 15 15 * 7 *").getSeriesFormatted(curTime, 2).toString());
        Assertions.assertEquals("[2024-07-02T15:15:15.000, 2024-07-09T15:15:15.000]", new Cron("15 15 15 * 7 2").getSeriesFormatted(curTime, 2).toString());

    }

}