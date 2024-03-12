package ru.jamsys.task.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.IgnoreClassFinder;
import ru.jamsys.task.Task;
import ru.jamsys.task.instance.FlushStatisticToInfluxTask;

@SuppressWarnings("unused")
@Component
@IgnoreClassFinder
public class FlushStatisticToInfluxGenerator implements Generator {
    @Override
    public String getCronTemplate() {
        return "*/5";
    }

    @Override
    public Task getTask() {
        return new FlushStatisticToInfluxTask();
    }
}
