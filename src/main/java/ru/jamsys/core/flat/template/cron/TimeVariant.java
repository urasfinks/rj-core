package ru.jamsys.core.flat.template.cron;

import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilDateOld;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.extension.statistic.AvgMetric;

import java.util.*;

@Getter
public class TimeVariant {

    private final Map<TimeUnit, Integer> timeValue = new LinkedHashMap<>();
    private final List<TimeUnit> listEmptyTimeUnit;

    public TimeVariant(List<TimeUnit> listEmptyTimeUnit) {
        this.listEmptyTimeUnit = listEmptyTimeUnit;
    }

    public void set(TimeUnit calendarTimeUnit, Integer value) {
        timeValue.put(calendarTimeUnit, value);
    }

    public void init() {
        LinkedList<TimeUnit> keys = new LinkedList<>(timeValue.keySet());
        List<TimeUnit> after = new ArrayList<>();
        while (!keys.isEmpty()) {
            TimeUnit timeUnit = keys.pollLast();
            if (timeValue.get(timeUnit) == null) {
                after.add(timeUnit);
            } else {
                break;
            }
        }
        for (int i = 0; i < after.size() - 1; i++) {
            listEmptyTimeUnit.remove(after.get(i));
        }
    }

    public int getNext(long curTime, AvgMetric avgMetric, boolean debug) {
        if (debug) {
            UtilLog.printInfo(new HashMapBuilder<String, Object>()
                    .append("value", timeValue)
                    .append("emptyUnit", listEmptyTimeUnit)
            );
        }
        if (listEmptyTimeUnit.size() == 1 && listEmptyTimeUnit.getFirst().equals(TimeUnit.DAY_OF_WEEK)) {
            if (debug) {
                UtilLog.printInfo("Нет смысла перебирать дни недели, так как дата стабильна");
            }
            return -1;
        }
        for (TimeUnit curEmptyTimeUnit : listEmptyTimeUnit) {
            if (debug) {
                UtilLog.printInfo("====EmptyUnit " + curEmptyTimeUnit.getNameCache() + " for time: " + UtilDateOld.msFormat(curTime));
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(curTime);
            resetDynamicUnitBefore(calendar, curEmptyTimeUnit, debug);
            if (debug) {
                UtilLog.printInfo("reset: " + UtilDateOld.msFormat(calendar.getTimeInMillis()));
            }
            timeInsert(calendar);
            if (debug) {
                UtilLog.printInfo("timeInsert: " + UtilDateOld.msFormat(calendar.getTimeInMillis()));
            }
            int count = 0;
            //Если мы сейчас переставляем дни месяца, то надо проверить наличие присутствия дня недели
            //Если день недели есть
            // 1) Он установился через timeInsert
            // 2) Надо начинать добавлять по 7, что бы перескакивать на целую неделю
            boolean weekIteration = curEmptyTimeUnit.equals(TimeUnit.DAY_OF_MONTH) && timeValue.get(TimeUnit.DAY_OF_WEEK) != null;
            if (!weekIteration) {
                curEmptyTimeUnit.setValue(calendar, curEmptyTimeUnit.getMin());
            }

            if (!curEmptyTimeUnit.equals(TimeUnit.DAY_OF_MONTH) && !curEmptyTimeUnit.equals(TimeUnit.DAY_OF_WEEK)) {
                Calendar now = Calendar.getInstance();
                now.setTimeInMillis(curTime);
                curEmptyTimeUnit.setValue(calendar, curEmptyTimeUnit.getValue(now));
                if (debug) {
                    UtilLog.printInfo("preSet: " + curEmptyTimeUnit + " = " + curEmptyTimeUnit.getValue(now) + " " + UtilDateOld.msFormat(calendar.getTimeInMillis()));
                }
            }

            for (int i = curEmptyTimeUnit.getMin(); i <= curEmptyTimeUnit.getMax(); i++) {
                long n = calendar.getTimeInMillis();
                if (n > curTime) {
                    if (debug) {
                        UtilLog.printInfo("[" + UtilDateOld.msFormat(n) + "] because > curTime: " + UtilDateOld.msFormat(curTime) + " realMs: " + n);
                    }
                    avgMetric.add(n);
                    if (listEmptyTimeUnit.size() == 1 && count == 0) {
                        return count;
                    }
                    break;
                }
                curEmptyTimeUnit.addValue(calendar, weekIteration ? 7 : 1);
                count++;
            }
            if (debug) {
                UtilLog.printInfo("total count: " + count);
            }
        }
        return -1;
    }

    public void resetDynamicUnitBefore(Calendar calendar, TimeUnit curTimeUnit, boolean debug) {
        TimeUnit.MILLISECOND.setValue(calendar, 0);
        for (TimeUnit vectorTimeUnit : Cron.getSequenceTimeUnit()) {
            if (vectorTimeUnit.equals(curTimeUnit)) {
                break;
            }
            // Не конкурентная проверка
            if (!listEmptyTimeUnit.contains(vectorTimeUnit)) {
                if (debug) {
                    UtilLog.printInfo(">r Unit." + vectorTimeUnit.getNameCache() + " continue because it is value template");
                }
                continue;
            }
            if (debug) {
                UtilLog.printInfo(">r " + vectorTimeUnit.getNameCache() + " = 0");
            }
            vectorTimeUnit.setValue(calendar, 0);
        }
    }

    public void timeInsert(Calendar calendar) {
        for (TimeUnit calendarTimeUnit : timeValue.keySet()) {
            Integer unitValue = timeValue.get(calendarTimeUnit);
            if (unitValue != null) {
                calendarTimeUnit.setValue(calendar, unitValue);
            }
        }
    }

    @Override
    public String toString() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (TimeUnit timeUnit : timeValue.keySet()) {
            result.put(timeUnit.getNameCache(), timeValue.get(timeUnit));
        }
        return result.toString();
    }

}