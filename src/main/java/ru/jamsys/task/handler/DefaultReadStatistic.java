package ru.jamsys.task.handler;

import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.ApplicationInit;
import ru.jamsys.broker.Queue;
import ru.jamsys.component.Broker;
import ru.jamsys.component.Generator;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.task.AbstractTaskHandler;
import ru.jamsys.task.Task;

import java.util.concurrent.atomic.AtomicBoolean;


public class DefaultReadStatistic extends AbstractTaskHandler implements ApplicationInit {
    @Override
    public void applicationInit() {

    }

    @Override
    public void run(Task task, AtomicBoolean isRun) throws Exception {
        Queue<StatisticSec> queue = App.context.getBean(Broker.class).get(StatisticSec.class);
        while (!queue.isEmpty() && isRun.get()) {
            queue.pollFirst();
        }
    }

    @Override
    public long getTimeoutMs() {
        return 5000;
    }
}
