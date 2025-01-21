package ru.jamsys.core.flat.template.cron;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.statistic.AvgMetric;

import java.util.*;

public class Cron {

    @Getter
    public static List<TimeUnit> sequenceTimeUnit = Arrays.asList(
            TimeUnit.SECOND,
            TimeUnit.MINUTE,
            TimeUnit.HOUR_OF_DAY,
            TimeUnit.DAY_OF_MONTH,
            TimeUnit.MONTH,
            TimeUnit.DAY_OF_WEEK
    );

    private final Map<Integer, TimeUnitContainer> fields = new LinkedHashMap<>(); //key: index sequence; value: TemplateTimeUnit

    @Getter
    private final Set<TimeVariant> listTimeVariant = new LinkedHashSet<>();

    @Getter
    private Long nextTimestamp = 0L;

    public Cron(String template) {
        for (int i = 0; i < sequenceTimeUnit.size(); i++) {
            fields.put(i, new TimeUnitContainer(sequenceTimeUnit.get(i)));
        }
        parseTemplate(template);
        initListTimeVariant();
    }

    private void parseTemplate(String template) {
        String[] s = template.split(" ");
        for (int i = 0; i < s.length; i++) {
            parseTemplateItem(s[i], fields.get(i));
        }
    }

    private void parseTemplateItem(String template, TimeUnitContainer timeUnitContainer) {
        if (template.indexOf(",") > 0) {
            String[] split = template.split(",");
            for (String s : split) {
                parseTemplateItem(s, timeUnitContainer);
            }
        } else {
            if (template.startsWith("*/")) {
                int inc = Integer.parseInt(template.substring(2));
                if (inc == 1) {
                    return;
                }
                int start = timeUnitContainer.getTimeUnit().getMin();
                int count = 0;
                while (true) {
                    timeUnitContainer.add(start);
                    start += inc;
                    if (start > timeUnitContainer.getTimeUnit().getMax()) {
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
                    timeUnitContainer.add(i);
                }
            } else if (!"*".equals(template)) {
                timeUnitContainer.add(Integer.parseInt(template));
            }
        }
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
            CompileResult compile = compile(curTime, false);
            if (!compile.isTimeHasCome()) {
                break;
            }
            curTime = compile.getNextTimestamp();
            result.add(curTime);
        }
        return result;
    }

    private void initListTimeVariant() {
        List<TimeUnit> listEmptyTimeUnit = new ArrayList<>();
        List<List<Integer>> tmpList = new ArrayList<>();
        for (Integer index : fields.keySet()) {
            TimeUnitContainer timeUnitContainer = fields.get(index);
            List<Integer> list = new ArrayList<>(timeUnitContainer.getList());
            if (list.isEmpty()) {
                listEmptyTimeUnit.add(timeUnitContainer.getTimeUnit());
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
                    timeVariant.set(fields.get(0).getTimeUnit(), second);
                    timeVariant.set(fields.get(1).getTimeUnit(), minute);
                    timeVariant.set(fields.get(2).getTimeUnit(), hour);
                    timeVariant.set(fields.get(3).getTimeUnit(), dayOfMonth);
                    timeVariant.set(fields.get(4).getTimeUnit(), monthOfYear);
                    timeVariant.set(fields.get(5).getTimeUnit(), dayOfWeek);
                    timeVariant.init();

                    listTimeVariant.add(timeVariant);
                }
        ))))));
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class CompileResult {

        Long beforeTimestamp; //Время до компиляции

        Long nextTimestamp; // Время после компиляции

        @SuppressWarnings("unused")
        public String getFormatBeforeTimestamp() {
            return UtilDate.msFormat(beforeTimestamp);
        }

        @SuppressWarnings("unused")
        public String getFormatNextTimestamp() {
            return UtilDate.msFormat(nextTimestamp);
        }

        public boolean isTimeHasCome() {
            if (nextTimestamp == null) {
                return false;
            }
            return !nextTimestamp.equals(beforeTimestamp);
        }

    }

    public CompileResult compile(long curTime) {
        return compile(curTime, false);
    }

    public CompileResult compile(long curTime, boolean debug) {
        // Если занулен либо где-то дальше от текущего момента - закончили
        CompileResult compileResult = new CompileResult().setBeforeTimestamp(nextTimestamp);
        if (nextTimestamp == null || nextTimestamp > curTime) {
            return compileResult
                    .setNextTimestamp(nextTimestamp);
        }
        AvgMetric avgMetric = new AvgMetric();
        for (TimeVariant timeVariant : listTimeVariant) {
            if (timeVariant.getNext(curTime, avgMetric, debug) == 0) {
                break;
            }
        }
        AvgMetric.Flush flush = avgMetric.flushInstance();
        if (flush.getCount() == 0) {
            // Если по каким-то причинам не было определено ни одного варианта в будущем
            // Этого момента не настанет, зануляем и compile больше никогда не вызовется
            nextTimestamp = null;
            return compileResult.setNextTimestamp(null);
        }
        if (debug) {
            Util.logConsole(
                    getClass(),
                    "Avg min: " + UtilDate.msFormat(flush.getMin()) + " realMs: " + flush.getMin()
            );
        }
        nextTimestamp = flush.getMin();
        return compileResult.setNextTimestamp(nextTimestamp);
    }

    @Override
    public String toString() {
        Map<String, TimeUnitContainer> result = new LinkedHashMap<>();
        for (Integer key : fields.keySet()) {
            TimeUnitContainer timeUnitContainer = fields.get(key);
            result.put(timeUnitContainer.getName(), timeUnitContainer);
        }
        return "Template(" + result + ")";
    }

}
