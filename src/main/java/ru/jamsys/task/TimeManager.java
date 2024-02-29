package ru.jamsys.task;

import lombok.Data;
import ru.jamsys.Util;
import ru.jamsys.UtilJson;
import ru.jamsys.statistic.AvgMetric;

import java.util.HashMap;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@Data
public class TimeManager {
    private final ConcurrentLinkedDeque<TaskStatisticExecute> queue = new ConcurrentLinkedDeque<>();

    public void flush() {
        Map<String, AvgMetric> mapByTask = new HashMap<>();
        Map<String, LongSummaryStatistics> result = new HashMap<>();
        TaskStatisticExecute[] taskStatisticExecutes = queue.toArray(new TaskStatisticExecute[0]);
        for (TaskStatisticExecute taskStatisticExecute : taskStatisticExecutes) {
            String taskIndex = taskStatisticExecute.getTask().getIndex();
            if (!mapByTask.containsKey(taskIndex)) {
                mapByTask.put(taskIndex, new AvgMetric());
            }
            if (taskStatisticExecute.getTimeExecute() != null) {
                queue.remove(taskStatisticExecute);
                mapByTask.get(taskIndex).add(taskStatisticExecute.getTimeExecute());
            } else {
                long currentTime = System.currentTimeMillis() - taskStatisticExecute.getTimeStart();
                if (currentTime > taskStatisticExecute.getTask().getTimeOutExecuteMillis()) {
                    Util.logConsole("Alarm!! Task: " + taskIndex + " timeOut: " + currentTime + " > " + taskStatisticExecute.getTask().getTimeOutExecuteMillis());
                    taskStatisticExecute.getExecutorService().reload();
                }
                mapByTask.get(taskIndex).add(currentTime);
            }
        }
        for (String key : mapByTask.keySet()) {
            result.put(key, mapByTask.get(key).flush());
        }
        System.out.println(UtilJson.toStringPretty(result, "{}"));
    }

    //public void addTask
}
