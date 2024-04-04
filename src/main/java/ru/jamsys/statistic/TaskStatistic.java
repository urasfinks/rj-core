package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.pool.AbstractPoolResource;
import ru.jamsys.thread.task.AbstractTask;

@Getter
@Setter
public class TaskStatistic extends TimeControllerImpl {

    AbstractPoolResource<?> resource;

    AbstractTask task;

    public TaskStatistic(AbstractPoolResource<?> resource, @Nullable AbstractTask task) {
        this.resource = resource;
        this.task = task;
    }

}
