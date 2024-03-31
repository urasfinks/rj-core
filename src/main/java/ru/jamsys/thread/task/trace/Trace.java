package ru.jamsys.thread.task.trace;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.thread.handler.Handler;
import ru.jamsys.thread.task.AbstractTask;

@Getter
@Setter
@ToString
public class Trace {
    long timeMs;
    TraceEvent traceEvent;
    Handler<? extends AbstractTask> abstractHandler;

    public Trace(long timeMs, TraceEvent traceEvent, Handler<? extends AbstractTask> abstractHandler) {
        this.timeMs = timeMs;
        this.traceEvent = traceEvent;
        this.abstractHandler = abstractHandler;
    }
}
