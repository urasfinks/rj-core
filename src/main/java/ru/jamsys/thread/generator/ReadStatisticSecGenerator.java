package ru.jamsys.thread.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.statistic.TimeEnvelope;
import ru.jamsys.thread.task.AbstractTask;
import ru.jamsys.thread.task.StatisticSecFlush;

@SuppressWarnings("unused")
@Component
public class ReadStatisticSecGenerator implements Generator {
    @Override
    public String getCronTemplate() {
        return "*/5";
    }

    @Override
    public TimeEnvelope<AbstractTask> getTaskTimeEnvelope() {
        TimeEnvelope<AbstractTask> timeEnvelope = new TimeEnvelope<>(new StatisticSecFlush());
        timeEnvelope.setKeepAliveOnInactivityMs(5_000);
        return timeEnvelope;
    }

    @Override
    public int getId() {
        return 1;
    }

}
