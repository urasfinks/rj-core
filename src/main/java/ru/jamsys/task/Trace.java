package ru.jamsys.task;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Trace {
    long timestamp;
    TraceEvent traceEvent;
    TaskHandler taskHandler;

    public Trace(long timestamp, TraceEvent traceEvent, TaskHandler taskHandler) {
        this.timestamp = timestamp;
        this.traceEvent = traceEvent;
        this.taskHandler = taskHandler;
    }
}
