package ru.jamsys.template.cron;

import lombok.Getter;
import ru.jamsys.extension.EnumName;

import java.util.Calendar;

public enum Unit implements EnumName {

    MILLISECOND(0, 999, Calendar.MILLISECOND),
    SECOND(0, 59, Calendar.SECOND),
    MINUTE(0, 59, Calendar.MINUTE),
    HOUR_OF_DAY(0, 23, Calendar.HOUR_OF_DAY),
    DAY_OF_MONTH(1, 31, Calendar.DAY_OF_MONTH),
    MONTH(1, 12, Calendar.MONTH),
    DAY_OF_WEEK(1, 7, Calendar.DAY_OF_WEEK);

    @Getter
    final
    int min;

    @Getter
    final
    int max;

    private final int calendarUnit;

    @Getter
    private final String nameCache;

    Unit(int min, int max, int calendarUnit) {
        this.min = min;
        this.max = max;
        this.calendarUnit = calendarUnit;
        this.nameCache = getName();
    }

    @SuppressWarnings("unused")
    public int getValue(Calendar calendar) {
        return switch (this) {
            case MONTH -> calendar.get(calendarUnit) + 1;
            case DAY_OF_WEEK -> calendar.get(calendarUnit) - 1;
            default -> calendar.get(calendarUnit);
        };
    }

    public void addValue(Calendar calendar, int value) {
        calendar.add(calendarUnit, value);
    }

    public void setValue(Calendar calendar, int value) {
        switch (this) {
            case MONTH -> calendar.set(calendarUnit, value - 1);
            case DAY_OF_WEEK -> calendar.set(calendarUnit, value + 1);
            default -> calendar.set(calendarUnit, value);
        }
        calendar.getTimeInMillis(); //Магическая штука, прогоните unit test без неё если захотите убедится в этом
    }
}
