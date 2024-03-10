package ru.jamsys.template.cron;

import lombok.Getter;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.util.Util;

import java.util.*;

public class Cron {

    @Getter
    public static List<Unit> vector = Arrays.asList(
            Unit.SECOND,
            Unit.MINUTE,
            Unit.HOUR_OF_DAY,
            Unit.DAY_OF_MONTH,
            Unit.MONTH,
            Unit.DAY_OF_WEEK
    );

    private final Map<Integer, TemplateItem> templateMap = new LinkedHashMap<>();

    private final List<TimeVariant> listTimeVariant = new ArrayList<>();

    private Long next = 0L;

    private final String template;

    public Cron(String template) {
        this.template = template;
        for (int i = 0; i < vector.size(); i++) {
            templateMap.put(i, new TemplateItem(vector.get(i)));
        }
        parseTemplate();
        init();
    }

    private void parseTemplate() {
        String[] s = this.template.split(" ");
        for (int i = 0; i < s.length; i++) {
            parseItem(s[i], templateMap.get(i));
        }
    }

    private void parseItem(String template, TemplateItem templateItem) {
        if (template.indexOf(",") > 0) {
            String[] split = template.split(",");
            for (String s : split) {
                parseItem(s, templateItem);
            }
        } else {
            if (template.startsWith("*/")) {
                int inc = Integer.parseInt(template.substring(2));
                if (inc == 1) {
                    return;
                }
                int start = templateItem.getUnit().getMin();
                int count = 0;
                while (true) {
                    templateItem.add(start);
                    start += inc;
                    if (start > templateItem.getUnit().getMax()) {
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
                    templateItem.add(i);
                }
            } else if (!"*".equals(template)) {
                templateItem.add(Integer.parseInt(template));
            }
        }
    }

    public Long getNext(long curTime, boolean debug) {
        long result = next;
        compile(curTime, debug);
        return result;
    }

    public Long getNext(long curTime) {
        return getNext(curTime, false);
    }

    public List<String> getSeriesFormatted(long curTime, int count) {
        List<String> result = new ArrayList<>();
        List<Long> series = getSeries(curTime, count);
        for (Long ms : series) {
            result.add(Util.msToDataFormat(ms));
        }
        return result;
    }

    public List<Long> getSeries(long curTime, int count) {
        List<Long> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long x = getNext(curTime, false);
            result.add(x);
            curTime = x;
        }
        return result;
    }

    public List<TimeVariant> getListTimeVariant() {
        return listTimeVariant;
    }

    private void init() {
        listTimeVariant.clear();
        List<Unit> listEmptyUnit = new ArrayList<>();
        List<List<Integer>> tmpList = new ArrayList<>();
        for (Integer index : templateMap.keySet()) {
            TemplateItem templateItem = templateMap.get(index);
            List<Integer> list = new ArrayList<>(templateItem.getList());
            if (list.isEmpty()) {
                listEmptyUnit.add(templateItem.getUnit());
                list.add(null);
            }
            tmpList.add(list);
        }

        tmpList.get(0).forEach((Integer second)
                -> tmpList.get(1).forEach((Integer minute)
                -> tmpList.get(2).forEach((Integer hour)
                -> tmpList.get(3).forEach((Integer dayOfMonth)
                -> tmpList.get(4).forEach((Integer monthOfYear)
                -> tmpList.get(5).forEach((Integer dayOfWeek) -> {
                    TimeVariant timeVariant = new TimeVariant(listEmptyUnit);
                    timeVariant.set(templateMap.get(0).getUnit(), second);
                    timeVariant.set(templateMap.get(1).getUnit(), minute);
                    timeVariant.set(templateMap.get(2).getUnit(), hour);
                    timeVariant.set(templateMap.get(3).getUnit(), dayOfMonth);
                    timeVariant.set(templateMap.get(4).getUnit(), monthOfYear);
                    timeVariant.set(templateMap.get(5).getUnit(), dayOfWeek);
                    timeVariant.init();

                    if (!listTimeVariant.contains(timeVariant)) {
                        listTimeVariant.add(timeVariant);
                    }
                }
        ))))));

    }

    public void compile(long curTime, boolean debug) {
        if (next != null && next <= curTime) {
            AvgMetric avgMetric = new AvgMetric();
            for (TimeVariant timeVariant : listTimeVariant) {
                if (timeVariant.getNext(curTime, avgMetric, debug) == 0) {
                    break;
                }
            }
            Map<String, Object> flush = avgMetric.flush("");
            if ((long) flush.get("Count") > 0) {
                next = (long) flush.get("Min");
            } else {
                next = null;
            }
        }
    }

    @Override
    public String toString() {
        Map<String, TemplateItem> result = new LinkedHashMap<>();
        for (Integer key : templateMap.keySet()) {
            TemplateItem templateItem = templateMap.get(key);
            result.put(templateItem.getName(), templateItem);
        }
        return "Template(" + result + ")";
    }
}
