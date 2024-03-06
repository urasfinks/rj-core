package ru.jamsys.task.generator.cron;

import lombok.Data;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.util.Util;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class UnitOne {

    Map<Integer, Integer> map = new LinkedHashMap<>();

    public void set(Integer calendarUnit, Integer value) {
        map.put(calendarUnit, value);
    }

    public void getNext(List<Unit> nulls, long curTime, AvgMetric avgMetric) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(curTime);

        nulls.forEach((Unit unit) -> {

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(curTime);
            timeInsert(calendar);
            //System.out.println("------ "+unit.getMapUnit().getName());
            resetFirst(calendar, unit);

            for (int i = now.get(unit.getMapUnit().getCalendarUnit()); i <= unit.getMax(); i++) {
                System.out.println("set " + unit.getMapUnit().getName() + "  = " + i);
                calendar.set(unit.getMapUnit().getCalendarUnit(), i);
                long n = calendar.getTimeInMillis();
                if (n > curTime) {
                    System.out.println("-" + Util.msToDataFormat(n));
                    avgMetric.add(n);
                    break;
                }
            }
        });
    }

    public void resetFirst(Calendar calendar, Unit unit) {
        for (MapUnit mapUnit : MapUnit.getVector()) {
            if (mapUnit.equals(unit.getMapUnit())) {
                return;
            }
            //System.out.println("reset " + mapUnit.getName() + " > 0");
            calendar.set(mapUnit.getCalendarUnit(), 0);
        }
    }

    public void timeInsert(Calendar calendar) {
        for (Integer calendarUnit : map.keySet()) {
            if (map.get(calendarUnit) != null) {
                calendar.set(calendarUnit, map.get(calendarUnit));
            }
        }
    }

    @Override
    public String toString() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Integer key : map.keySet()) {
            result.put(MapUnit.indexOf(key).getName(), map.get(key));
        }
        return result.toString();
    }
}