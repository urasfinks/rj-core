package ru.jamsys.component;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.task.handler.FlushStatistic;
import ru.jamsys.task.handler.ReadTaskHandlerStatistic;
import ru.jamsys.thread.ExecutorServiceScheduler;

@Component
@Lazy
@Getter
public class ExecutorService extends AbstractComponent {

    private final ExecutorServiceScheduler t1 = new ExecutorServiceScheduler(1000);
    private final ExecutorServiceScheduler t3 = new ExecutorServiceScheduler(3000);
    private final ExecutorServiceScheduler t5 = new ExecutorServiceScheduler(5000);

    public ExecutorService(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    public void run() {
        super.run();

        t1.getListAbstractTaskHandler().add(App.context.getBean(FlushStatistic.class));
        t1.getListAbstractTaskHandler().add(App.context.getBean(ReadTaskHandlerStatistic.class));

        t1.run();
        t3.run();

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
