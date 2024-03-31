package ru.jamsys.thread.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.thread.task.StatisticCollectorFlush;
import ru.jamsys.thread.task.AbstractTask;

@SuppressWarnings("unused")
@Component
public class FlushStatisticCollectorGenerator implements Generator {
    @Override
    public String getCronTemplate() {
        return "*";
    }

    @Override
    public AbstractTask getTask() {
        return new StatisticCollectorFlush(1_000);
    }

    @Override
    public int getId() {
        return 2;
    }
}
