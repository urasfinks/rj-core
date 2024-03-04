package ru.jamsys.task.handler;

import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.broker.Queue;
import ru.jamsys.component.Broker;
import ru.jamsys.component.TaskTiming;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.task.Task;
import ru.jamsys.task.TaskHandlerStatistic;
import ru.jamsys.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ReadTaskHandlerStatistic extends AbstractHandler {
    @Override
    public void run(Task task, AtomicBoolean isRun) throws Exception {
        /*
         * 1) Надо собратить статистику сколько было вызвано TaskHandler'ов за последнюю секунду
         * */

        Broker broker = App.context.getBean(Broker.class);
        Queue<TaskHandlerStatistic> queue = broker.get(TaskHandlerStatistic.class);
        Map<String, AvgMetric> mapTime = new HashMap<>();
        Map<String, AtomicLong> mapOperation = new HashMap<>();
        long currentTimeMs = System.currentTimeMillis();
        List<TaskHandlerStatistic> list = queue.getCloneQueue(isRun);

        for (TaskHandlerStatistic taskHandlerStatistic : list) {
            if (isRun.get()) {
                String taskIndex = taskHandlerStatistic.geComplexIndex();
                if (!mapTime.containsKey(taskIndex)) {
                    mapTime.put(taskIndex, new AvgMetric());
                }
                if (!taskHandlerStatistic.isWasProcessedStatistic()) {
                    if (!mapOperation.containsKey(taskIndex)) {
                        mapOperation.put(taskIndex, new AtomicLong(0));
                    }
                    mapOperation.get(taskIndex).incrementAndGet();
                    taskHandlerStatistic.setWasProcessedStatistic(true);
                }
                AvgMetric timeAvgMetric = mapTime.get(taskIndex);
                if (taskHandlerStatistic.getTimeExecuteMs() != null) {
                    timeAvgMetric.add(taskHandlerStatistic.getTimeExecuteMs());
                    queue.remove(taskHandlerStatistic);
                } else {
                    long curTimeMs = System.currentTimeMillis() - taskHandlerStatistic.getTimeStartMs();
                    timeAvgMetric.add(curTimeMs);
                    if (curTimeMs > taskHandlerStatistic.getTaskHandler().getTimeoutMs()) {
                        Util.logConsole("Alarm!! Task: " + taskIndex + " timeOut: " + curTimeMs + " > " + taskHandlerStatistic.getTaskHandler().getTimeoutMs());
                        //TODO: проверить timeOut и если что прибить поток
                    }
                }
            } else {
                break;
            }
        }
        TaskTiming taskTiming = App.context.getBean(TaskTiming.class);
        taskTiming.insert(currentTimeMs, mapTime, mapOperation);
        //result.forEach((String key, AvgMetric metric) -> taskTiming.addMetric(key, metric.flush("Time")));
    }

    @Override
    public long getTimeoutMs() {
        return 1000;
    }
}
