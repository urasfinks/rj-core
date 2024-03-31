package ru.jamsys.thread.generator;

import org.springframework.stereotype.Component;
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
    public AbstractTask getTask() {
        return new StatisticSecFlush(5_000);
    }

    @Override
    public int getId() {
        return 1;
    }

}
