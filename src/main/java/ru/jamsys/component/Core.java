package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import ru.jamsys.App;
import ru.jamsys.ApplicationInit;
import ru.jamsys.statistic.StatisticsCollector;
import ru.jamsys.task.Task;
import ru.jamsys.task.handler.TaskHandler;

import java.util.*;


@org.springframework.stereotype.Component
@Lazy
public class Core extends AbstractComponent implements ApplicationInit {

    private final Dictionary dictionary;

    public Core(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    public void applicationInit() {
        ClassFinder classFinder = App.context.getBean(ClassFinder.class);
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
                -> dictionary.getListApplicationInit().add(App.context.getBean(applicationInitClass)));

        dictionary.getListApplicationInit().forEach((ApplicationInit applicationInit) -> {
            if (!applicationInit.equals(this)) {
                applicationInit.applicationInit();
            }
        });
    }

}
