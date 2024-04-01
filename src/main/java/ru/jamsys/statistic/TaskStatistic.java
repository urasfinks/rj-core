package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.pool.AbstractPoolResource;
import ru.jamsys.thread.task.AbstractTask;

@Getter
@Setter
public class TaskStatistic implements BrokerCollectible {

    AbstractPoolResource<?> resource;

    AbstractTask task;

    long timeStartMs = System.currentTimeMillis();
    Long timeExecuteMs = null;

    public TaskStatistic(AbstractPoolResource<?> resource, @Nullable AbstractTask task) {
        this.resource = resource;
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
