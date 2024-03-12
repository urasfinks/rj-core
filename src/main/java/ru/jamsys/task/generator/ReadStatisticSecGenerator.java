package ru.jamsys.task.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.task.Task;
import ru.jamsys.task.instance.ReadStatisticSecTask;

@SuppressWarnings("unused")
@Component
public class ReadStatisticSecGenerator implements Generator {
    @Override
    public String getCronTemplate() {
        return "*/5";
    }

    @Override
    public Task getTask() {
        return new ReadStatisticSecTask();
    }
}
