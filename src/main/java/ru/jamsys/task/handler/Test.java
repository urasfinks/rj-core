package ru.jamsys.task.handler;

import ru.jamsys.App;
import ru.jamsys.component.Dictionary;
import ru.jamsys.task.instance.StatisticTask;

import java.util.concurrent.atomic.AtomicBoolean;

public class Test implements TaskHandler<StatisticTask> {

    @Override
    public void run(StatisticTask task, AtomicBoolean isRun) throws Exception {

    }

    public void register(){
        App.context.getBean(Dictionary.class).getTaskHandler().put(StatisticTask.class, Test.class);
    }

    @Override
    public long getTimeoutMs() {
        return 0;
    }
}
