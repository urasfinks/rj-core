package ru.jamsys.task.generator.cron;

import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public enum MapUnit {
    MILLISECOND(Calendar.MILLISECOND),
    SECOND(Calendar.SECOND),
    MINUTE(Calendar.MINUTE),
    HOUR(Calendar.HOUR),
    DAY_OF_MONTH(Calendar.DAY_OF_MONTH),
    MONTH(Calendar.MONTH),
    DAY_OF_WEEK(Calendar.DAY_OF_WEEK);

    int calendarUnit;

    MapUnit(int calendarUnit) {
        this.calendarUnit = calendarUnit;
    }

    String getName() {
        return Util.snakeToCamel(name());
    }

    int getCalendarUnit() {
        return calendarUnit;
    }

    static MapUnit indexOf(int calendarUnit) {
        for (MapUnit mapUnit : MapUnit.values()) {
            if (mapUnit.getCalendarUnit() == calendarUnit) {
                return mapUnit;
            }
        }
        return null;
    }

    static List<MapUnit> getVector() {
        List<MapUnit> result = new ArrayList<>();
        result.add(MapUnit.MILLISECOND);
        result.add(MapUnit.SECOND);
        result.add(MapUnit.MINUTE);
        result.add(MapUnit.HOUR);
        result.add(MapUnit.DAY_OF_MONTH);
        result.add(MapUnit.MONTH);
        result.add(MapUnit.DAY_OF_WEEK);
        return result;
    }
}
