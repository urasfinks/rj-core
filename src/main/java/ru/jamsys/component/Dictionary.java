package ru.jamsys.component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.RunnableComponent;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.template.cron.CronTask;
import ru.jamsys.thread.handler.Handler;
import ru.jamsys.thread.task.AbstractTask;

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

    List<StatisticsCollectorComponent> listStatisticsCollectorComponent = new ArrayList<>();

    List<RunnableComponent> listRunnableComponents = new ArrayList<>();

    List<KeepAliveComponent> listKeepAliveComponent = new ArrayList<>();

    List<CronTask> listCronTask = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    Map<Class<? extends AbstractTask>, Handler> taskHandler = new ConcurrentHashMap<>();
}
