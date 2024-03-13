package ru.jamsys.task.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.task.Task;
import ru.jamsys.task.task.FlushStatisticCollectorTask;

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
