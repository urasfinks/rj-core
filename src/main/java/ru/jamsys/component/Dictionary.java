package ru.jamsys.component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.RunnableComponent;
import ru.jamsys.statistic.StatisticsCollector;
import ru.jamsys.task.Task;
import ru.jamsys.task.handler.TaskHandler;

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

    @SuppressWarnings("rawtypes")
    Map<Class<? extends Task>, TaskHandler> taskHandler = new ConcurrentHashMap<>();
}
