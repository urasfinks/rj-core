package ru.jamsys.component;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import ru.jamsys.App;
import ru.jamsys.ApplicationInit;
import ru.jamsys.statistic.StatisticsCollector;
import ru.jamsys.task.Task;
import ru.jamsys.task.handler.TaskHandler;
import ru.jamsys.thread.Starter;

import java.util.*;


@org.springframework.stereotype.Component
@Lazy
public class Core extends AbstractComponent implements Starter {

    @Getter
    List<Class<? extends Starter>> list = new ArrayList<>();

    public Core() {
        list.add(Generator.class);
    }

    public void run() {
        applicationInit();
        for (Class<? extends Starter> cls : list) {
            App.context.getBean(cls).run();
        }
    }

    public void applicationInit() {
        App.context.getBean(Security.class).run();

        ClassFinder classFinder = App.context.getBean(ClassFinder.class);
        Dictionary dictionary = App.context.getBean(Dictionary.class);
        @SuppressWarnings("rawtypes")
        List<Class<TaskHandler>> list = classFinder.findByInstance(TaskHandler.class);
        for (Class<?> taskHandler : list) {
            List<Class<Task>> typeInterface = classFinder.getTypeInterface(taskHandler, Task.class);
            for (Class<Task> iClass : typeInterface) {
                dictionary.getTaskHandler().put(iClass, (TaskHandler<?>) App.context.getBean(taskHandler));
            }
        }
        classFinder.findByInstance(StatisticsCollector.class).forEach((Class<StatisticsCollector> statisticsCollector)
                -> dictionary.getListStatisticsCollector().add(App.context.getBean(statisticsCollector)));
        classFinder.findByInstance(ApplicationInit.class).forEach((Class<ApplicationInit> applicationInitClass)
                -> App.context.getBean(applicationInitClass).applicationInit());
    }

    @Override
    public void shutdown() {
        //Опускаем в обратной последовательности
        for (int i = list.size() - 1; i >= 0; i--) {
            App.context.getBean(list.get(i)).shutdown();
        }
    }

    @Override
    synchronized public void reload() {
        shutdown();
        run();
    }

}
