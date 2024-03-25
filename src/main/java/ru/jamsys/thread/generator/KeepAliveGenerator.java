package ru.jamsys.thread.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.thread.task.KeepAlive;
import ru.jamsys.thread.task.Task;

@SuppressWarnings("unused")
@Component
public class KeepAliveGenerator implements Generator {

    @Override
    public String getCronTemplate() {
        return "*/3";
    }

    @Override
    public Task getTask() {
        return new KeepAlive();
    }

    @Override
    public int getId() {
        return 0;
    }
}
