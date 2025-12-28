package ru.jamsys.core.flat.util;

import lombok.Getter;

import java.time.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class UtilDateBetween {
    /* ------------------------ TIME BETWEEN ------------------------ */

    public enum StyleDescription { STANDARD, FORMAL }

    public enum Unit {
        YEARS("год", "года", "лет"),
        MONTHS("месяц", "месяца", "месяцев"),
        DAYS("день", "дня", "дней"),
        HOURS("час", "часа", "часов"),
        MINUTES("минута", "минуты", "минут"),
        SECONDS("секунда", "секунды", "секунд");

        final String one;
        final String two;
        final String five;

        Unit(String one, String two, String five) {
            this.one = one;
            this.two = two;
            this.five = five;
        }

        public String digitTranslate(long count) {
            return UtilText.digitTranslate(count, one, two, five);
        }
    }

    @Getter
    public static final class TimeBetween {
        private final Map<Unit, Long> units = new EnumMap<>(Unit.class);

        public TimeBetween() {
            for (Unit u : Unit.values()) units.put(u, 0L);
        }

        public TimeBetween set(Unit unit, long value) {
            units.put(unit, value);
            return this;
        }

        public String getDescription(int count, StyleDescription style) {
            var list = new java.util.ArrayList<String>();
            for (Unit unit : Unit.values()) {
                long c = units.get(unit);
                if (c == 0) continue;
                list.add(c + " " + unit.digitTranslate(c));
                if (--count == 0) break;
            }
            if (list.isEmpty()) return null;

            return switch (style) {
                case STANDARD -> String.join(" ", list);
                case FORMAL -> {
                    if (list.size() == 1) yield list.getFirst();
                    String last = list.removeLast();
                    yield String.join(", ", list) + " и " + last;
                }
            };
        }

        public String getDescription() {
            return getDescription(6, StyleDescription.STANDARD);
        }
    }

    /**
     * Разложение разницы на годы/месяцы/дни/часы/мин/сек в рамках календаря указанной зоны.
     * Вход — именно epochMilli (а не epochSecond).
     */
    public static TimeBetween betweenEpochMillis(long startEpochMilli, long endEpochMilli, ZoneId zoneId) {
        Objects.requireNonNull(zoneId, "zoneId");

        Instant start = Instant.ofEpochMilli(startEpochMilli);
        Instant end = Instant.ofEpochMilli(endEpochMilli);

        boolean reversed = start.isAfter(end);
        if (reversed) {
            Instant tmp = start; start = end; end = tmp;
        }

        ZonedDateTime zStart = start.atZone(zoneId);
        ZonedDateTime zEnd   = end.atZone(zoneId);

        // Сначала календарная часть (до даты)
        Period period = Period.between(zStart.toLocalDate(), zEnd.toLocalDate());
        ZonedDateTime afterDate = zStart
                .plusYears(period.getYears())
                .plusMonths(period.getMonths())
                .plusDays(period.getDays());

        // Затем оставшаяся “временная” часть
        Duration duration = Duration.between(afterDate, zEnd);

        // На всякий случай (переходы DST могут дать отрицательную длительность после “календарного выравнивания”)
        if (duration.isNegative()) {
            afterDate = afterDate.minusDays(1);
            period = period.minusDays(1);
            duration = Duration.between(afterDate, zEnd);
        }

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        if (reversed) {
            years = -years; months = -months; days = -days;
            hours = -hours; minutes = -minutes; seconds = -seconds;
        }

        return new TimeBetween()
                .set(Unit.YEARS, years)
                .set(Unit.MONTHS, months)
                .set(Unit.DAYS, days)
                .set(Unit.HOURS, hours)
                .set(Unit.MINUTES, minutes)
                .set(Unit.SECONDS, seconds);
    }

    public static TimeBetween betweenEpochMillis(long startEpochMilli, long endEpochMilli) {
        return betweenEpochMillis(startEpochMilli, endEpochMilli, UtilDate.DEFAULT_ZONE);
    }

}
