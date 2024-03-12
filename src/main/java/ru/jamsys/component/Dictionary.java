package ru.jamsys.component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.annotation.Lazy;
import ru.jamsys.ApplicationInit;
import ru.jamsys.statistic.StatisticsCollector;
import ru.jamsys.task.Task;
import ru.jamsys.task.handler.TaskHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@org.springframework.stereotype.Component
@Lazy
@Getter
@Setter
@ToString
public class Dictionary {

    List<StatisticsCollector> listStatisticsCollector = new ArrayList<>();

    List<ApplicationInit> listApplicationInit = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    Map<Class<? extends Task>, TaskHandler> taskHandler = new ConcurrentHashMap<>();
}
