package ru.jamsys.template.cron;

import lombok.Data;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.util.Util;

import java.util.*;

@Data
public class TimeVariant {

    private Map<Unit, Integer> timeValue = new LinkedHashMap<>();
    private final List<Unit> listEmptyUnit;

    public TimeVariant(List<Unit> listEmptyUnit) {
        this.listEmptyUnit = listEmptyUnit;
    }

    public void set(Unit calendarUnit, Integer value) {
        timeValue.put(calendarUnit, value);
    }

    public void init() {
        LinkedList<Unit> keys = new LinkedList<>(timeValue.keySet());
        List<Unit> after = new ArrayList<>();
        while (!keys.isEmpty()) {
            Unit unit = keys.pollLast();
            if (timeValue.get(unit) == null) {
                after.add(unit);
            } else {
                break;
            }
        }
        for (int i = 0; i < after.size() - 1; i++) {
            listEmptyUnit.remove(after.get(i));
        }
    }

    public int getNext(long curTime, AvgMetric avgMetric, boolean debug) {
        if (debug) {
            System.out.println("---------------------------------------------------");
            System.out.println("value: " + timeValue);
            System.out.println("empty unit: " + listEmptyUnit);
        }
        if (listEmptyUnit.size() == 1 && listEmptyUnit.get(0).equals(Unit.DAY_OF_WEEK)) {
            if (debug) {
                System.out.println("Нет смысла перебирать дни недели, так как дата стабильна");
            }
            return -1;
        }
        for (Unit curEmptyUnit : listEmptyUnit) {
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
            //Если мы сейчас переставляем дни месяца, то надо проверить наличие присутствия дня недели
            //Если день недели есть
            // 1) Он установился через timeInsert
            // 2) Надо начинать добавлять по 7, что бы перескакивать на целую неделю
            boolean weekIteration = curEmptyUnit.equals(Unit.DAY_OF_MONTH) && timeValue.get(Unit.DAY_OF_WEEK) != null;
            if (!weekIteration) {
                curEmptyUnit.setValue(calendar, curEmptyUnit.getMin());
            }

            if (!curEmptyUnit.equals(Unit.DAY_OF_MONTH) && !curEmptyUnit.equals(Unit.DAY_OF_WEEK)) {
                Calendar now = Calendar.getInstance();
                now.setTimeInMillis(curTime);
                curEmptyUnit.setValue(calendar, curEmptyUnit.getValue(now));
                if (debug) {
                    System.out.println("preSet: " + curEmptyUnit + " = " + curEmptyUnit.getValue(now) + " " + Util.msToDataFormat(calendar.getTimeInMillis()));
                }
            }

            for (int i = curEmptyUnit.getMin(); i <= curEmptyUnit.getMax(); i++) {
                long n = calendar.getTimeInMillis();
                if (n > curTime) {
                    if (debug) {
                        System.out.println("[" + Util.msToDataFormat(n) + "] because > curTime: " + Util.msToDataFormat(curTime));
                    }
                    avgMetric.add(n);
                    if (listEmptyUnit.size() == 1 && count == 0) {
                        return count;
                    }
                    break;
                }
                curEmptyUnit.addValue(calendar, weekIteration ? 7 : 1);
                count++;
            }
            if (debug) {
                System.out.println("total count: " + count);
            }
        }
        return -1;
    }

    public void resetDynamicUnitBefore(Calendar calendar, Unit curUnit, boolean debug) {
        Unit.MILLISECOND.setValue(calendar, 0);
        for (Unit vectorUnit : Cron.getVector()) {
            if (vectorUnit.equals(curUnit)) {
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