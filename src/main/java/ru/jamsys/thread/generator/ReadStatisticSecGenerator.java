package ru.jamsys.thread.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.thread.task.StatisticSecFlush;
import ru.jamsys.thread.task.Task;

@SuppressWarnings("unused")
@Component
public class ReadStatisticSecGenerator implements Generator {
    @Override
    public String getCronTemplate() {
        return "*/5";
    }

    @Override
    public Task getTask() {
        return new StatisticSecFlush(5_000);
    }

    @Override
    public int getId() {
        return 1;
    }

}
