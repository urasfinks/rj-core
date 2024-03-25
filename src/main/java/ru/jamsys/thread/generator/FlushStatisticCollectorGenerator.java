package ru.jamsys.thread.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.thread.task.StatisticCollectorFlush;
import ru.jamsys.thread.task.Task;

@SuppressWarnings("unused")
@Component
public class FlushStatisticCollectorGenerator implements Generator {
    @Override
    public String getCronTemplate() {
        return "*";
    }

    @Override
    public Task getTask() {
        return new StatisticCollectorFlush();
    }

    @Override
    public int getId() {
        return 2;
    }
}
