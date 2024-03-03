package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.task.handler.LoadStatisticToInfluxDb;
import ru.jamsys.task.handler.StatisticCollectorHandler;
import ru.jamsys.task.handler.ReadTaskHandlerStatistic;
import ru.jamsys.thread.ExecutorServiceScheduler;

@Component
@Lazy
public class ExecutorService extends AbstractComponent {

    ExecutorServiceScheduler t1 = new ExecutorServiceScheduler(1000);
    ExecutorServiceScheduler t3 = new ExecutorServiceScheduler(3000);
    ExecutorServiceScheduler t5 = new ExecutorServiceScheduler(5000);

    @Override
    public void run() {
        super.run();

        t1.getListTaskHandler().add(App.context.getBean(StatisticCollectorHandler.class));
        t1.getListTaskHandler().add(App.context.getBean(ReadTaskHandlerStatistic.class));

        t1.run();
        t3.run();

        t5.getListTaskHandler().add(App.context.getBean(LoadStatisticToInfluxDb.class));
        t5.run();

    }

    @Override
    public void shutdown() {
        super.shutdown();
        t1.shutdown();
        t3.shutdown();
        t5.shutdown();
    }

}
