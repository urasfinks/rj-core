package ru.jamsys.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.RunnableComponent;
import ru.jamsys.RunnableInterface;
import ru.jamsys.statistic.StatisticsCollector;
import ru.jamsys.task.Task;
import ru.jamsys.task.handler.TaskHandler;

import java.util.*;


@Component
@Lazy
public class Core implements RunnableInterface {

    private final Dictionary dictionary;

    public Core(ApplicationContext applicationContext, Dictionary dictionary, ClassFinder classFinder) {
        this.dictionary = dictionary;
        @SuppressWarnings("rawtypes")
        List<Class<TaskHandler>> list = classFinder.findByInstance(TaskHandler.class);
        for (Class<?> taskHandler : list) {
            List<Class<Task>> typeInterface = classFinder.getTypeInterface(taskHandler, Task.class);
            for (Class<Task> iClass : typeInterface) {
                dictionary.getTaskHandler().put(iClass, (TaskHandler<?>) applicationContext.getBean(taskHandler));
            }
        }
        classFinder.findByInstance(StatisticsCollector.class).forEach((Class<StatisticsCollector> statisticsCollector) -> {
            if (!classFinder.instanceOf(this.getClass(), statisticsCollector)) {
                dictionary.getListStatisticsCollector().add(applicationContext.getBean(statisticsCollector));
            }
        });

        classFinder.findByInstance(RunnableComponent.class).forEach((Class<RunnableComponent> runnableComponentClass) -> {
            if (!classFinder.instanceOf(this.getClass(), runnableComponentClass)) {
                dictionary.getListRunnableComponents().add(applicationContext.getBean(runnableComponentClass));
            }
        });
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
