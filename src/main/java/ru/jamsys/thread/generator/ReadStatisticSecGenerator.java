package ru.jamsys.thread.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.thread.task.Task;
import ru.jamsys.thread.task.ReadStatisticSecTask;

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

    @Override
    public int getId() {
        return 1;
    }

}
