package ru.jamsys.task;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Trace {
    long timeMs;
    TraceEvent traceEvent;
    TaskHandler taskHandler;

    public Trace(long timeMs, TraceEvent traceEvent, TaskHandler taskHandler) {
        this.timeMs = timeMs;
        this.traceEvent = traceEvent;
        this.taskHandler = taskHandler;
    }
}
