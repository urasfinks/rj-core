package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.thread.handler.Handler;
import ru.jamsys.thread.task.Task;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public class TaskStatistic implements BrokerCollectible {
    Thread thread;
    Task task;
    Handler<? extends Task> handler;

    long timeStartMs = System.currentTimeMillis();
    Long timeExecuteMs = null;

    AtomicBoolean firstPrepare = new AtomicBoolean(true); //Было обработано статистикой

    @SuppressWarnings("unused")
    public boolean isFirstPrepare() {
        return firstPrepare.compareAndSet(true, false);
    }

    public TaskStatistic(Thread thread, @Nullable Task task, Handler<? extends Task> abstractHandler) {
        this.thread = thread;
        this.task = task;
        this.handler = abstractHandler;
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
        return timeExecuteMs == null && (System.currentTimeMillis() - timeStartMs) > handler.getTimeoutMs();
    }

    @SuppressWarnings("unused")
    public boolean isFinished() {
        return timeExecuteMs != null;
    }


}
