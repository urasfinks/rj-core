package ru.jamsys.component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.RunnableComponent;
import ru.jamsys.StatisticsCollector;
import ru.jamsys.thread.task.Task;
import ru.jamsys.thread.handler.Handler;
import ru.jamsys.template.cron.CronTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Lazy
@Getter
@Setter
@ToString
public class Dictionary {

    List<StatisticsCollector> listStatisticsCollector = new ArrayList<>();

    List<RunnableComponent> listRunnableComponents = new ArrayList<>();

    List<CronTask> listCronTask = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    Map<Class<? extends Task>, Handler> taskHandler = new ConcurrentHashMap<>();
}
