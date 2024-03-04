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
    AbstractTaskHandler taskHandler;

    long timeStartMs = System.currentTimeMillis();
    Long timeExecuteMs = null;

    boolean wasProcessedStatistic = false; //Было обработано статистикой

    public TaskHandlerStatistic(Thread thread, @Nullable Task task, AbstractTaskHandler abstractTaskHandler) {
        this.thread = thread;
        this.task = task;
        this.taskHandler = abstractTaskHandler;
    }

    public Long getTimeExecuteMs() {
        return timeExecuteMs != null ? timeExecuteMs : System.currentTimeMillis() - timeStartMs;
    }

    public void finish() {
        timeExecuteMs = System.currentTimeMillis() - timeStartMs;
    }

    public boolean isTimeout() {
        return timeExecuteMs == null && (System.currentTimeMillis() - timeStartMs) > taskHandler.getTimeoutMs();
    }

}
