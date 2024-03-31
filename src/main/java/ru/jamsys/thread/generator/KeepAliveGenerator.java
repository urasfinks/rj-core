package ru.jamsys.thread.generator;

import org.springframework.stereotype.Component;
import ru.jamsys.thread.task.AbstractTask;
import ru.jamsys.thread.task.KeepAlive;

@SuppressWarnings("unused")
@Component
public class KeepAliveGenerator implements Generator {

    @Override
    public String getCronTemplate() {
        return "*/3";
    }

    @Override
    public AbstractTask getTask() {
        return new KeepAlive(60_000);
    }

    @Override
    public int getId() {
        return 0;
    }
}
