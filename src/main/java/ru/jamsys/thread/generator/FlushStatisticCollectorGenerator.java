package ru.jamsys.thread.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.thread.task.Task;
import ru.jamsys.thread.task.FlushStatisticCollectorTask;

@SuppressWarnings("unused")
@Component
public class FlushStatisticCollectorGenerator implements Generator {
    @Override
    public String getCronTemplate() {
        return "*";
    }

    @Override
    public Task getTask() {
        return new FlushStatisticCollectorTask();
    }
}
