package ru.jamsys.task.generator.cron;

import lombok.Getter;
import lombok.ToString;
import ru.jamsys.statistic.AvgMetric;

import java.util.*;

@ToString
public class Template {

    @Getter
    Unit second = new Unit(0, 59, MapUnit.SECOND); // Секунды [0-59]

    @Getter
    Unit minute = new Unit(0, 59, MapUnit.MINUTE); // Минуты [0-59]

    @Getter
    Unit hour = new Unit(0, 23, MapUnit.HOUR); // Часы [0-23]

    @Getter
    Unit dayOfMonth = new Unit(1, 31, MapUnit.DAY_OF_MONTH); // Дни месяца [1-31]

    @Getter
    Unit monthOfYear = new Unit(1, 12, MapUnit.MONTH); // Месяц года

    @Getter
    Unit dayOfWeek = new Unit(1, 7, MapUnit.DAY_OF_WEEK); // Дни недели

    @ToString.Exclude
    private final Map<Integer, Unit> map = new LinkedHashMap<>();

    @ToString.Exclude
    List<UnitOne> cartesian = new ArrayList<>();

    @ToString.Exclude
    List<Unit> nulls = new ArrayList<>();

    @ToString.Exclude
    long next = 0;


    public Template(String template) {
        map.put(0, second);
        map.put(1, minute);
        map.put(2, hour);
        map.put(3, dayOfMonth);
        map.put(4, monthOfYear);
        map.put(5, dayOfWeek);
        parse(template);
    }

    private void parse(String template) {
        String[] s = template.split(" ");
        for (int i = 0; i < s.length; i++) {
            parseItem(s[i], map.get(i));
        }
    }

    private void parseItem(String template, Unit unit) {
        if (template.indexOf(",") > 0) {
            String[] split = template.split(",");
            for (String s : split) {
                parseItem(s, unit);
            }
        } else {
            if (template.startsWith("*/")) {
                int inc = Integer.parseInt(template.substring(2));
                int start = unit.getMin();
                int count = 0;
                while (true) {
                    unit.add(start);
                    start += inc;
                    if (start > unit.getMax()) {
                        break;
                    }
                    count++;
                    if (count > 60) {
                        break;
                    }
                }
            } else if (template.indexOf("-") > 0) {
                String[] split = template.split("-");
                int start = Integer.parseInt(split[0]);
                int end = Integer.parseInt(split[1]);
                for (int i = start; i <= end; i++) {
                    unit.add(i);
                }
            } else if (!"*".equals(template)) {
                unit.add(Integer.parseInt(template));
            }
        }
    }

    public long getNext(long curTime) {
        compile(curTime);
        return next;
    }

    public List<UnitOne> getCartesian() {
        return cartesian;
    }

    public Template compile(long curTime) {
        if (next < curTime) {
            if (cartesian.isEmpty()) {
                for (Integer idx : map.keySet()) {
                    Unit unit = map.get(idx);
                    if (unit.getList().isEmpty()) {
                        nulls.add(unit);
                        unit.getList().add(null);
                    }
                }

                List<Integer> secondList = map.get(0).getList();
                List<Integer> minuteList = map.get(1).getList();
                List<Integer> hourList = map.get(2).getList();
                List<Integer> dayOfMonthList = map.get(3).getList();
                List<Integer> monthOfYearList = map.get(4).getList();
                List<Integer> dayOfWeekList = map.get(5).getList();

                secondList.forEach((Integer second)
                        -> minuteList.forEach((Integer minute)
                        -> hourList.forEach((Integer hour)
                        -> dayOfMonthList.forEach((Integer dayOfMonth)
                        -> monthOfYearList.forEach((Integer monthOfYear)
                        -> dayOfWeekList.forEach((Integer dayOfWeek) -> {
                            UnitOne unitOne = new UnitOne();
                            unitOne.set(map.get(0).getMapUnit().getCalendarUnit(), second);
                            unitOne.set(map.get(1).getMapUnit().getCalendarUnit(), minute);
                            unitOne.set(map.get(2).getMapUnit().getCalendarUnit(), hour);
                            unitOne.set(map.get(3).getMapUnit().getCalendarUnit(), dayOfMonth);
                            unitOne.set(map.get(4).getMapUnit().getCalendarUnit(), monthOfYear);
                            unitOne.set(map.get(5).getMapUnit().getCalendarUnit(), dayOfWeek);

                            if (!cartesian.contains(unitOne)) {
                                cartesian.add(unitOne);
                            }
                        }
                ))))));
            }
            AvgMetric avgMetric = new AvgMetric();
            for (UnitOne unitOne : cartesian) {
                unitOne.getNext(nulls, curTime, avgMetric);
            }
            next = (long) avgMetric.flush("").get("Min");
        }
        return this;
    }

}
