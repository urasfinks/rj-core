package ru.jamsys.task.handler;


import org.springframework.stereotype.Component;
import ru.jamsys.task.instance.StatisticTask;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class Test implements TaskHandler<StatisticTask> {

    @Override
    public void run(StatisticTask task, AtomicBoolean isRun) throws Exception {

    }

    @Override
    public long getTimeoutMs() {
        return 0;
    }
}
