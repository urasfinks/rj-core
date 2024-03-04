package ru.jamsys.task.handler;

import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.broker.Queue;
import ru.jamsys.component.Broker;
import ru.jamsys.component.TaskTiming;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.task.TagIndex;
import ru.jamsys.task.Task;
import ru.jamsys.task.AbstractTaskHandler;
import ru.jamsys.task.TaskHandlerStatistic;
import ru.jamsys.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ReadTaskHandlerStatistic extends AbstractTaskHandler {

    @Override
    public void run(Task task, AtomicBoolean isRun) throws Exception {
        Broker broker = App.context.getBean(Broker.class);
        Queue<TaskHandlerStatistic> queue = broker.get(TaskHandlerStatistic.class);
        List<TaskHandlerStatistic> list = queue.getCloneQueue(isRun);

        Map<String, AvgMetric> taskTime = new HashMap<>();
        Map<String, AvgMetric> taskHandlerTime = new HashMap<>();

        list.forEach((TaskHandlerStatistic taskHandlerStatistic) -> {
            long timeExecuteMs = taskHandlerStatistic.getTimeExecuteMs();
            addIdExist(taskHandlerStatistic.getTaskHandler(), taskHandlerTime, timeExecuteMs);
            if (taskHandlerStatistic.getTask() != null) {
                addIdExist(taskHandlerStatistic.getTaskHandler(), taskTime, timeExecuteMs);
            }
            if (taskHandlerStatistic.isTimeout()) {
                Util.logConsole("Alarm!! TaskHandler: " + taskHandlerStatistic.getTaskHandler().getIndex() + " timeOut: execute=" + timeExecuteMs + " > timeOut=" + taskHandlerStatistic.getTaskHandler().getTimeoutMs());
                //TODO: проверить timeOut и если что прибить поток
            }
        });
        TaskTiming taskTiming = App.context.getBean(TaskTiming.class);
        taskTiming.insert(taskTime, taskHandlerTime);
    }

    private void addIdExist(TagIndex tagIndex, Map<String, AvgMetric> map, Long unitValue) {
        String index = tagIndex.getIndex();
        if (!map.containsKey(index)) {
            map.put(index, new AvgMetric());
        }
        map.get(index).add(unitValue);
    }

    @Override
    public long getTimeoutMs() {
        return 1000;
    }
}
