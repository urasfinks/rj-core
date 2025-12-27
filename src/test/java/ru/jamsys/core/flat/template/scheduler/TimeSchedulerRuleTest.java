package ru.jamsys.core.flat.template.scheduler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilDate;

import java.time.ZoneId;

class TimeSchedulerRuleTest {

    @Test
    public void test() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [],
                  "months": [],
                  "days": [],
                  "hours": [],
                  "minutes": [],
                  "seconds": [],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [],
                  "days_of_week": []
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2025-12-27T13:32:45.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }

    @Test
    public void test2() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [],
                  "months": [],
                  "days": [],
                  "hours": [],
                  "minutes": [],
                  "seconds": [47],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [],
                  "days_of_week": []
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2025-12-27T13:32:47.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }

    @Test
    public void test3() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [],
                  "months": [],
                  "days": [],
                  "hours": [],
                  "minutes": [33],
                  "seconds": [],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [],
                  "days_of_week": []
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2025-12-27T13:33:00.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }

    @Test
    public void test4() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [],
                  "months": [],
                  "days": [],
                  "hours": [],
                  "minutes": [33],
                  "seconds": [47],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [],
                  "days_of_week": []
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2025-12-27T13:33:47.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }

    @Test
    public void test5() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [],
                  "months": [],
                  "days": [],
                  "hours": [12],
                  "minutes": [33],
                  "seconds": [47],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [],
                  "days_of_week": []
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2025-12-28T12:33:47.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }

    @Test
    public void test6() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [],
                  "months": [],
                  "days": [],
                  "hours": [12],
                  "minutes": [33],
                  "seconds": [47],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [],
                  "days_of_week": [1]
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2025-12-29T12:33:47.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }

    @Test
    public void test7() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [],
                  "months": [],
                  "days": [],
                  "hours": [12],
                  "minutes": [33],
                  "seconds": [47],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [1],
                  "days_of_week": [1]
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2026-01-05T12:33:47.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }

    @Test
    public void test8() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [],
                  "months": [],
                  "days": [1],
                  "hours": [12],
                  "minutes": [33],
                  "seconds": [47],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [1],
                  "days_of_week": [1]
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2027-02-01T12:33:47.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }

    @Test
    public void test9() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [],
                  "months": [],
                  "days": [28],
                  "hours": [12],
                  "minutes": [33],
                  "seconds": [47],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [],
                  "days_of_week": [7]
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2025-12-28T12:33:47.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }

    @Test
    public void test10() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [],
                  "months": [],
                  "days": [],
                  "hours": [],
                  "minutes": [],
                  "seconds": [],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [],
                  "days_of_week": [1]
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2025-12-29T00:00:00.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }

    @Test
    public void test11() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [2026],
                  "months": [],
                  "days": [],
                  "hours": [],
                  "minutes": [],
                  "seconds": [],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [],
                  "days_of_week": []
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2026-01-01T00:00:00.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }

    @Test
    public void test12() throws Exception {
        TimeSchedulerRule timeSchedulerRule = TimeSchedulerRule.fromJson("""
                {
                  "title": "description",
                  "start": 1766827964000,
                  "years": [2026],
                  "months": [2],
                  "days": [],
                  "hours": [],
                  "minutes": [],
                  "seconds": [],
                  "truncate_time": false,
                  "offset": 0,
                  "exclude": [],
                  "quarters": [],
                  "days_of_week": []
                }""");
        TimeSchedulerPlan timeSchedulerPlan = new TimeSchedulerPlan(timeSchedulerRule, ZoneId.systemDefault());
        Assertions.assertEquals("2026-02-01T00:00:00.000", UtilDate.msFormat(timeSchedulerPlan.nextAfter(1766827964000L)));
    }


}