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
    AbstractTaskHandler abstractTaskHandler;

    public Trace(long timeMs, TraceEvent traceEvent, AbstractTaskHandler abstractTaskHandler) {
        this.timeMs = timeMs;
        this.traceEvent = traceEvent;
        this.abstractTaskHandler = abstractTaskHandler;
    }
}
