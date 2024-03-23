package ru.jamsys.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.RunnableComponent;
import ru.jamsys.extension.RunnableInterface;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.template.cron.Cron;
import ru.jamsys.template.cron.CronTask;
import ru.jamsys.thread.generator.Generator;
import ru.jamsys.thread.handler.Handler;
import ru.jamsys.thread.task.Task;
import ru.jamsys.util.ListSort;

import java.util.List;


@Component
@Lazy
public class Core implements RunnableInterface {

    private final Dictionary dictionary;

    public Core(ApplicationContext applicationContext, Dictionary dictionary, ClassFinder classFinder) {
        this.dictionary = dictionary;
        @SuppressWarnings("rawtypes")
        List<Class<Handler>> list = classFinder.findByInstance(Handler.class);
        for (Class<?> taskHandler : list) {
            List<Class<Task>> typeInterface = classFinder.getTypeInterface(taskHandler, Task.class);
            for (Class<Task> iClass : typeInterface) {
                dictionary.getTaskHandler().put(iClass, (Handler<?>) applicationContext.getBean(taskHandler));
            }
        }
        classFinder.findByInstance(StatisticsCollectorComponent.class).forEach((Class<StatisticsCollectorComponent> statisticsCollectorClass) -> {
            if (!classFinder.instanceOf(this.getClass(), statisticsCollectorClass)) {
                dictionary.getListStatisticsCollectorComponent().add(applicationContext.getBean(statisticsCollectorClass));
            }
        });

        classFinder.findByInstance(KeepAliveComponent.class).forEach((Class<KeepAliveComponent> keepAliveClass) -> {
            if (!classFinder.instanceOf(this.getClass(), keepAliveClass)) {
                dictionary.getListKeepAliveComponent().add(applicationContext.getBean(keepAliveClass));
            }
        });

        classFinder.findByInstance(RunnableComponent.class).forEach((Class<RunnableComponent> runnableComponentClass) -> {
            if (!classFinder.instanceOf(this.getClass(), runnableComponentClass)) {
                dictionary.getListRunnableComponents().add(applicationContext.getBean(runnableComponentClass));
            }
        });

        ListSort<CronTask> generatorListSort = new ListSort<>();
        classFinder.findByInstance(Generator.class).forEach((Class<Generator> generatorClass) -> {
            if (!classFinder.instanceOf(this.getClass(), generatorClass)) {
                Generator generator = applicationContext.getBean(generatorClass);
                generatorListSort.add(generator.getId(), new CronTask(
                        new Cron(generator.getCronTemplate()),
                        generator.getTask()
                ));
            }
        });
        generatorListSort.getSorted().forEach(cronTask -> dictionary.getListCronTask().add(cronTask));
    }

    @Override
    public void run() {
        dictionary.getListRunnableComponents().forEach(RunnableInterface::run);
    }

    @Override
    public void shutdown() {
        dictionary.getListRunnableComponents().forEach(RunnableInterface::shutdown);
    }

    @Override
    synchronized public void reload() {
        shutdown();
        run();
    }
}
