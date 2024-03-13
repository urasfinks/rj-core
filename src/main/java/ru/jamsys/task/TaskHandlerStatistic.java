package ru.jamsys.task;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.App;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.component.Broker;
import ru.jamsys.task.handler.TaskHandler;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public class TaskHandlerStatistic implements BrokerCollectible {
    Thread thread;
    Task task;
    TaskHandler<? extends Task> taskHandler;

    long timeStartMs = System.currentTimeMillis();
    Long timeExecuteMs = null;

    AtomicBoolean firstPrepare = new AtomicBoolean(true); //Было обработано статистикой

    @SuppressWarnings("unused")
    public boolean isFirstPrepare() {
        return firstPrepare.compareAndSet(true, false);
    }

    public TaskHandlerStatistic(Thread thread, @Nullable Task task, TaskHandler<? extends Task> abstractTaskHandler) {
        this.thread = thread;
        this.task = task;
        this.taskHandler = abstractTaskHandler;
    }

    @SuppressWarnings("unused")
    public Long getTimeExecuteMs() {
        return timeExecuteMs != null ? timeExecuteMs : System.currentTimeMillis() - timeStartMs;
    }

    @SuppressWarnings("unused")
    public void finish() {
        timeExecuteMs = System.currentTimeMillis() - timeStartMs;
    }

    @SuppressWarnings("unused")
    public boolean isTimeout() {
        return timeExecuteMs == null && (System.currentTimeMillis() - timeStartMs) > taskHandler.getTimeoutMs();
    }

    @SuppressWarnings("unused")
    public boolean isFinished() {
        return timeExecuteMs != null;
    }

    public static void clearOnStopThread(Thread thread) {
        Broker broker = App.context.getBean(Broker.class);
        ru.jamsys.broker.Queue<TaskHandlerStatistic> taskHandlerStatisticQueue = broker.get(TaskHandlerStatistic.class);
        Util.riskModifierCollection(
                null,
                taskHandlerStatisticQueue.getCloneQueue(null),
                new TaskHandlerStatistic[0],
                (TaskHandlerStatistic taskStatisticExecute) -> {
                    if (taskStatisticExecute.getThread().equals(thread)) {
                        taskHandlerStatisticQueue.remove(taskStatisticExecute);
                    }
                });
    }

}
