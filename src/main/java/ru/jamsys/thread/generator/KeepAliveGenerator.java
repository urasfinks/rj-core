package ru.jamsys.thread.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.statistic.TimeEnvelope;
import ru.jamsys.thread.task.AbstractTask;
import ru.jamsys.thread.task.KeepAlive;

@SuppressWarnings("unused")
@Component
public class KeepAliveGenerator implements Generator {

    @Override
    public String getCronTemplate() {
        return "*/3";
    }

    @Override
    public TimeEnvelope<AbstractTask> getTaskTimeEnvelope() {
        TimeEnvelope<AbstractTask> timeEnvelope = new TimeEnvelope<>(new KeepAlive());
        timeEnvelope.setKeepAliveOnInactivityMs(60_000);
        return timeEnvelope;
    }

    @Override
    public int getId() {
        return 0;
    }
}
