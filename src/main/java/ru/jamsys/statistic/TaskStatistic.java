package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.task.Task;

@Getter
@Setter
public class TaskStatistic implements BrokerCollectible {

    ThreadEnvelope threadEnvelope;

    Task task;

    long timeStartMs = System.currentTimeMillis();
    Long timeExecuteMs = null;

    public TaskStatistic(ThreadEnvelope threadEnvelope, @Nullable Task task) {
        this.threadEnvelope = threadEnvelope;
        this.task = task;
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
    public boolean isFinished() {
        return timeExecuteMs != null;
    }


}
