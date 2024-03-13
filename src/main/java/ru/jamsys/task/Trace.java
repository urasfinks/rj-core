package ru.jamsys.task;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.task.handler.TaskHandler;

@Getter
@Setter
@ToString
public class Trace {
    long timeMs;
    TraceEvent traceEvent;
    TaskHandler<? extends Task> abstractTaskHandler;

    public Trace(long timeMs, TraceEvent traceEvent, TaskHandler<? extends Task> abstractTaskHandler) {
        this.timeMs = timeMs;
        this.traceEvent = traceEvent;
        this.abstractTaskHandler = abstractTaskHandler;
    }
}
