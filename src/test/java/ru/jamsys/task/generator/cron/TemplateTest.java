package ru.jamsys.task.generator.cron;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.util.Util;

class TemplateTest {

    @Test
    public void test() {
        Assertions.assertEquals("Template(second=[30], minute=[8], hour=[10], dayOfMonth=[6], monthOfYear=[], dayOfWeek=[])", new Template("30 08 10 06 *").toString());
        Assertions.assertEquals("Template(second=[1], minute=[], hour=[], dayOfMonth=[], monthOfYear=[], dayOfWeek=[])", new Template("1 * * * *").toString());
        Assertions.assertEquals("Template(second=[1, 2], minute=[], hour=[], dayOfMonth=[], monthOfYear=[], dayOfWeek=[])", new Template("1-2 * * * *").toString());
        Assertions.assertEquals("Template(second=[1, 2, 3], minute=[], hour=[], dayOfMonth=[], monthOfYear=[], dayOfWeek=[])", new Template("1-3 * * * *").toString());
        Assertions.assertEquals("Template(second=[0, 15, 30, 45], minute=[], hour=[], dayOfMonth=[], monthOfYear=[], dayOfWeek=[])", new Template("*/15 * * * *").toString());
        Assertions.assertEquals("Template(second=[1, 0, 15, 30, 45], minute=[], hour=[], dayOfMonth=[], monthOfYear=[], dayOfWeek=[])", new Template("1,*/15 * * * *").toString());
        Assertions.assertEquals("Template(second=[1, 2, 3, 0, 15, 30, 45], minute=[], hour=[], dayOfMonth=[], monthOfYear=[], dayOfWeek=[])", new Template("1-3,*/15 * * * *").toString());
        Assertions.assertEquals("Template(second=[0, 1, 2, 3, 15, 30, 45], minute=[], hour=[], dayOfMonth=[], monthOfYear=[], dayOfWeek=[])", new Template("0-3,*/15 * * * *").toString());
        Assertions.assertEquals("Template(second=[20, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34, 35], minute=[], hour=[], dayOfMonth=[], monthOfYear=[], dayOfWeek=[])", new Template("20-25,30-35 * * * *").toString());
        Assertions.assertEquals("Template(second=[], minute=[], hour=[], dayOfMonth=[], monthOfYear=[], dayOfWeek=[])", new Template("60 * * * *").toString());
        Assertions.assertEquals("Template(second=[59], minute=[], hour=[], dayOfMonth=[], monthOfYear=[], dayOfWeek=[])", new Template("59,60,61 * * * *").toString());

        long curTime = 1709734264056L; //2024-03-06T17:11:04.056
        Assertions.assertEquals("[{Second=1, Minute=null, Hour=null, DayOfMonth=null, Month=null, DayOfWeek=null}]", new Template("1 * * * *").compile(curTime).getCartesian().toString());

        Assertions.assertEquals("2024-03-06T17:12:01", Util.msToDataFormat(new Template("1 * * * *").getNext(curTime)));
//        Assertions.assertEquals("2024-03-06T17:12:02", Util.msToDataFormat(new Template("2 * * * *").getNext(curTime)));
//        Assertions.assertEquals("2024-03-06T17:11:05", Util.msToDataFormat(new Template("* * * * *").getNext(curTime)));
//        Assertions.assertEquals("2024-03-06T17:11:05", Util.msToDataFormat(new Template("*/1 * * * *").getNext(curTime)));
//        Assertions.assertEquals("2024-03-06T17:11:15", Util.msToDataFormat(new Template("*/15 * * * *").getNext(curTime)));
//        Assertions.assertEquals("2024-03-06T18:00", Util.msToDataFormat(new Template("* 0 * * *").getNext(curTime)));
//        Assertions.assertEquals("2024-03-06T18:01", Util.msToDataFormat(new Template("* 1 * * *").getNext(curTime)));
//        Assertions.assertEquals("2024-03-06T18:01", Util.msToDataFormat(new Template("* * 1 * *").getNext(curTime)));
    }

}