package ru.jamsys.thread.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.statistic.TimeEnvelope;
import ru.jamsys.thread.task.AbstractTask;
import ru.jamsys.thread.task.StatisticCollectorFlush;

@SuppressWarnings("unused")
@Component
public class FlushStatisticCollectorGenerator implements Generator {
    @Override
    public String getCronTemplate() {
        return "*";
    }

    @Override
    public TimeEnvelope<AbstractTask> getTaskTimeEnvelope() {
        TimeEnvelope<AbstractTask> timeEnvelope = new TimeEnvelope<>(new StatisticCollectorFlush());
        timeEnvelope.setKeepAliveOnInactivityMs(1_000);
        return timeEnvelope;
    }

    @Override
    public int getId() {
        return 2;
    }
}
