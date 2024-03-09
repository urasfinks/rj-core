package ru.jamsys.task.generator.cron;

import lombok.Data;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.util.Util;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class TimeVariant {

    Map<Unit, Integer> timeValue = new LinkedHashMap<>();
    final List<Unit> listEmptyUnit;

    public TimeVariant(List<Unit> listEmptyUnit) {
        this.listEmptyUnit = listEmptyUnit;
    }

    public void set(Unit calendarUnit, Integer value) {
        timeValue.put(calendarUnit, value);
    }

    public void getNext(long curTime, AvgMetric avgMetric, boolean debug) {
        if (debug) {
            System.out.println("---------------------------------------------------");
            System.out.println(timeValue);
        }
        if (listEmptyUnit.size() == 1 && listEmptyUnit.get(0).equals(Unit.DAY_OF_WEEK)) {
            if (debug) {
                System.out.println("Нет смысла перебирать дни недели, так как дата стабильна");
            }
            return;
        }
        if (listEmptyUnit.isEmpty()) {
            if (debug) {
                System.out.println("Дата стабильна");
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(curTime);
            timeInsert(calendar);
            avgMetric.add(calendar.getTimeInMillis());
            return;
        }
        listEmptyUnit.forEach((Unit curEmptyUnit) -> {
            if (debug) {
                System.out.println("====EmptyUnit " + curEmptyUnit.getName() + " for time: " + Util.msToDataFormat(curTime));
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(curTime);
            resetDynamicUnitBefore(calendar, curEmptyUnit, debug);
            if (debug) {
                System.out.println("reset: " + Util.msToDataFormat(calendar.getTimeInMillis()));
            }
            timeInsert(calendar);
            if (debug) {
                System.out.println("timeInsert: " + Util.msToDataFormat(calendar.getTimeInMillis()));
            }
            int count = 0;
            //Если мы сейчас переставляем дня месяца, то надо проверить наличие присудствия дня недели
            //Если день недели есть
            // 1) Он установился через timeInsert
            // 2) Надо начинать добавлять по 7, что бы перескакивать на целую неделю
            boolean weekIteration = curEmptyUnit.equals(Unit.DAY_OF_MONTH) && !listEmptyUnit.contains(Unit.DAY_OF_WEEK);
            if (!weekIteration) {
                curEmptyUnit.setValue(calendar, curEmptyUnit.getMin());
            }
            for (int i = curEmptyUnit.getMin(); i <= curEmptyUnit.getMax(); i++) {
                long n = calendar.getTimeInMillis();
                if (n > curTime) {
                    if (debug) {
                        System.out.println("[" + Util.msToDataFormat(n) + "] because > curTime: " + Util.msToDataFormat(curTime));
                    }
                    avgMetric.add(n);
                    break;
                }
                curEmptyUnit.addValue(calendar, weekIteration ? 7 : 1);
                count++;
            }
            if (debug) {
                System.out.println("total count: " + count);
            }
        });
    }

    public void resetDynamicUnitBefore(Calendar calendar, Unit unitEmpty, boolean debug) {
        Unit.MILLISECOND.setValue(calendar, 0);
        for (Unit vectorUnit : Unit.getVector()) {
            if (vectorUnit.equals(unitEmpty)) {
                break;
            }
            if (!listEmptyUnit.contains(vectorUnit)) {
                if (debug) {
                    System.out.println(">r Unit." + vectorUnit.getName() + " continue because it is value template");
                }
                continue;
            }
            if (debug) {
                System.out.println(">r " + vectorUnit.getName() + " = 0");
            }
            vectorUnit.setValue(calendar, 0);
        }
    }

    public void timeInsert(Calendar calendar) {
        for (Unit calendarUnit : timeValue.keySet()) {
            Integer unitValue = timeValue.get(calendarUnit);
            if (unitValue != null) {
                calendarUnit.setValue(calendar, unitValue);
            }
        }
    }

    @Override
    public String toString() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Unit unit : timeValue.keySet()) {
            result.put(unit.getName(), timeValue.get(unit));
        }
        return result.toString();
    }
}