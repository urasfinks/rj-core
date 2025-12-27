package ru.jamsys.core.flat.template.period;

import lombok.Getter;

import java.time.Duration;
import java.time.Period;

/*
* P[n]Y[n]M[n]DT[n]H[n]M[n]S
*
* P — начало duration
* T — разделитель date/time части
*
*** Date-часть (до T) ***
* [n]Y  — годы //P1Y
* [n]M  — месяцы //P2M
* [n]D  — дни //P10D
* P1Y2M10D

*** Time-часть (после T) ***
* [n]H  — часы //PT12H
* [n]M  — минуты //PT30M
* [n]S  — секунды //PT10S
* PT1H30M / PT1.5S

*** ВАЖНО ***
* IsoDuration не имеет привязок к календарю, он не знает дни недели! Нельзя сделать шаблон на каждый понедельник
* P1M     = 1 месяц
* PT1M    = 1 минута
* P1MT12H ≠ (не равно) каждый месяц в 12:00 (ШАБЛОН НАКОПИТЕЛЬНЫЙ!!!)
 * */

@Getter
public class IsoDuration {

    private final Period period;
    private final Duration duration;
    private final String iso;

    private IsoDuration(Period period, Duration duration, String iso) {
        this.period = period;
        this.duration = duration;
        this.iso = iso;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ---------------- Builder ----------------

    public static final class Builder {
        private int years;
        private int months;
        private int days;
        private int hours;
        private int minutes;
        private int seconds;
        private int nanos;

        private boolean used = false;

        public Builder years(int v) {
            check(v, "years");
            years = v;
            used = true;
            return this;
        }

        public Builder months(int v) {
            check(v, "months");
            months = v;
            used = true;
            return this;
        }

        public Builder days(int v) {
            check(v, "days");
            days = v;
            used = true;
            return this;
        }

        public Builder hours(int v) {
            check(v, "hours");
            hours = v;
            used = true;
            return this;
        }

        public Builder minutes(int v) {
            check(v, "minutes");
            minutes = v;
            used = true;
            return this;
        }

        public Builder seconds(int v) {
            check(v, "seconds");
            seconds = v;
            used = true;
            return this;
        }

        public Builder nanos(int v) {
            if (v < 0 || v > 999_999_999) {
                throw new IllegalArgumentException("nanos must be between 0 and 999,999,999");
            }
            nanos = v;
            used = true;
            return this;
        }

        public IsoDuration build() {
            if (!used) {
                // канонический ноль ISO 8601
                return new IsoDuration(Period.ZERO, Duration.ZERO, "PT0S");
            }

            Period p = Period.of(years, months, days);
            Duration d = Duration.ofHours(hours)
                    .plusMinutes(minutes)
                    .plusSeconds(seconds)
                    .plusNanos(nanos);

            String iso = buildIsoString();

            return new IsoDuration(p, d, iso);
        }

        // ---------- helpers ----------

        private String buildIsoString() {
            StringBuilder sb = new StringBuilder();
            sb.append('P');

            if (years != 0) sb.append(years).append('Y');
            if (months != 0) sb.append(months).append('M');
            if (days != 0) sb.append(days).append('D');

            boolean hasTime =
                    hours != 0 || minutes != 0 || seconds != 0 || nanos != 0;

            if (hasTime) {
                sb.append('T');
                if (hours != 0) sb.append(hours).append('H');
                if (minutes != 0) sb.append(minutes).append('M');

                if (seconds != 0 || nanos != 0) {
                    if (nanos == 0) {
                        sb.append(seconds).append('S');
                    } else {
                        String frac = String.format("%09d", nanos);
                        frac = stripTrailingZeros(frac);
                        sb.append(seconds).append('.').append(frac).append('S');
                    }
                }
            }

            // если указана только date-часть — "P1D" валидно
            // если только time-часть — "PT..."
            return sb.toString();
        }

        private static String stripTrailingZeros(String s) {
            int i = s.length();
            while (i > 0 && s.charAt(i - 1) == '0') i--;
            return s.substring(0, i);
        }

        private static void check(int v, String name) {
            if (v < 0) {
                throw new IllegalArgumentException(name + " must be >= 0");
            }
        }
    }

    public static IsoDuration parse(String iso) {
        if (iso == null || iso.isBlank()) throw new IllegalArgumentException("iso is blank");
        if (!iso.startsWith("P")) throw new IllegalArgumentException("Not an ISO-8601 duration: " + iso);

        String[] parts = iso.split("T", 2);
        String datePart = parts[0];                 // "P1M"
        String timePart = (parts.length == 2) ? "PT" + parts[1] : null; // "PT12H"

        Period p = Period.ZERO;
        if (!datePart.equals("P")) {
            // Period.parse умеет PnYnMnD (без T)
            p = Period.parse(datePart);
        }

        Duration d = Duration.ZERO;
        if (timePart != null && !timePart.equals("PT")) {
            // Duration.parse умеет PTnHnMnS (и PnD тоже, но мы оставляем дни в Period)
            d = Duration.parse(timePart);
        }
        return new IsoDuration(p, d, iso);
    }
}
