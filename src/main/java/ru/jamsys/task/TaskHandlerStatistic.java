package ru.jamsys.task;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.broker.BrokerCollectible;

@Getter
@Setter
public class TaskHandlerStatistic implements BrokerCollectible {
    Thread thread;
    Task task;
    TaskHandler taskHandler;

    long timeStart = System.currentTimeMillis();
    Long timeExecute = null;

    boolean wasProcessedStatistic = false; //Было обработано статистикой

    public TaskHandlerStatistic(Thread thread, @Nullable Task task, TaskHandler taskHandler) {
        this.thread = thread;
        this.task = task;
        this.taskHandler = taskHandler;
    }

    public String geComplexIndex() {
        return taskHandler.getIndex() + (task != null ? "-" + task.getIndex() : "");
    }

    public void finish() {
        timeExecute = System.currentTimeMillis() - timeStart;
    }

}
