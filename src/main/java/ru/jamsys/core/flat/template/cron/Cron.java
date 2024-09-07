package ru.jamsys.core.flat.template.cron;

import lombok.Getter;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.AvgMetricUnit;

import java.util.*;

public class Cron {

    @Getter
    public static List<TimeUnit> vector = Arrays.asList(
            TimeUnit.SECOND,
            TimeUnit.MINUTE,
            TimeUnit.HOUR_OF_DAY,
            TimeUnit.DAY_OF_MONTH,
            TimeUnit.MONTH,
            TimeUnit.DAY_OF_WEEK
    );

    private final Map<Integer, TemplateItemCron> templateMap = new LinkedHashMap<>();

    @Getter
    private final Set<TimeVariant> listTimeVariant = new LinkedHashSet<>();

    private Long next = 0L;

    private final String template;

    public Cron(String template) {
        this.template = template;
        for (int i = 0; i < vector.size(); i++) {
            templateMap.put(i, new TemplateItemCron(vector.get(i)));
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

    private void parseItem(String template, TemplateItemCron templateItemCron) {
        if (template.indexOf(",") > 0) {
            String[] split = template.split(",");
            for (String s : split) {
                parseItem(s, templateItemCron);
            }
        } else {
            if (template.startsWith("*/")) {
                int inc = Integer.parseInt(template.substring(2));
                if (inc == 1) {
                    return;
                }
                int start = templateItemCron.getTimeUnit().getMin();
                int count = 0;
                while (true) {
                    templateItemCron.add(start);
                    start += inc;
                    if (start > templateItemCron.getTimeUnit().getMax()) {
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
                    templateItemCron.add(i);
                }
            } else if (!"*".equals(template)) {
                templateItemCron.add(Integer.parseInt(template));
            }
        }
    }

    public boolean isTimeHasCome(long curTime) {
        boolean result = next <= curTime;
        compile(curTime, false);
        return result;
    }

    public Long getNext(long curTime, boolean debug) {
        compile(curTime, debug);
        return next;
    }

    public Long getNext(long curTime) {
        return getNext(curTime, false);
    }

    public List<String> getSeriesFormatted(long curTime, int count) {
        List<String> result = new ArrayList<>();
        List<Long> series = getSeries(curTime, count);
        for (Long ms : series) {
            result.add(UtilDate.msFormat(ms));
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

    private void init() {

        listTimeVariant.clear();
        List<TimeUnit> listEmptyTimeUnit = new ArrayList<>();
        List<List<Integer>> tmpList = new ArrayList<>();
        for (Integer index : templateMap.keySet()) {
            TemplateItemCron templateItemCron = templateMap.get(index);
            List<Integer> list = new ArrayList<>(templateItemCron.getList());
            if (list.isEmpty()) {
                listEmptyTimeUnit.add(templateItemCron.getTimeUnit());
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
                    TimeVariant timeVariant = new TimeVariant(listEmptyTimeUnit);
                    timeVariant.set(templateMap.get(0).getTimeUnit(), second);
                    timeVariant.set(templateMap.get(1).getTimeUnit(), minute);
                    timeVariant.set(templateMap.get(2).getTimeUnit(), hour);
                    timeVariant.set(templateMap.get(3).getTimeUnit(), dayOfMonth);
                    timeVariant.set(templateMap.get(4).getTimeUnit(), monthOfYear);
                    timeVariant.set(templateMap.get(5).getTimeUnit(), dayOfWeek);
                    timeVariant.init();

                    listTimeVariant.add(timeVariant);
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
            if ((long) flush.get(AvgMetricUnit.SELECTION.getNameCache()) > 0) {
                if (debug) {
                    System.out.println("Avg min: "
                            + UtilDate.msFormat((Long) flush.get(AvgMetricUnit.MIN.getNameCache()))
                            + " realMs: "
                            + flush.get(AvgMetricUnit.MIN.getNameCache())
                    );
                }
                next = (long) flush.get(AvgMetricUnit.MIN.getNameCache());
            } else {
                next = null;
            }
        }
    }

    @Override
    public String toString() {
        Map<String, TemplateItemCron> result = new LinkedHashMap<>();
        for (Integer key : templateMap.keySet()) {
            TemplateItemCron templateItemCron = templateMap.get(key);
            result.put(templateItemCron.getName(), templateItemCron);
        }
        return "Template(" + result + ")";
    }
}
