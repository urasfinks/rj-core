package ru.jamsys.task;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Data
@EqualsAndHashCode(callSuper = false)
public class Task extends TagIndex {
    private final Consumer<AtomicBoolean> consumer;
    private final long timeOutExecuteMillis;

    public Task(Consumer<AtomicBoolean> consumer, long timeOutExecuteMillis) {
        this.consumer = consumer;
        this.timeOutExecuteMillis = timeOutExecuteMillis; //TimeManager будет кидать interrupt если что
    }

    public void onKill(){

    }

}
