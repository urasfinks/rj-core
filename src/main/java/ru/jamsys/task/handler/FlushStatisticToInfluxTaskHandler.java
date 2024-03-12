package ru.jamsys.task.handler;


import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.task.instance.FlushStatisticToInfluxTask;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
public class FlushStatisticToInfluxTaskHandler implements TaskHandler<FlushStatisticToInfluxTask> {

    @Override
    public void run(FlushStatisticToInfluxTask task, AtomicBoolean isRun) throws Exception {
        Util.logConsole("!!!");
    }

    @Override
    public long getTimeoutMs() {
        return 1000;
    }
}
