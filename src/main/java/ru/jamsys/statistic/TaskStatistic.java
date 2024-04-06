package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.pool.PoolItem;
import ru.jamsys.thread.task.AbstractTask;

@Getter
@Setter
public class TaskStatistic extends TimeControllerImpl {

    PoolItem<?> poolItem;

    AbstractTask task;

    public TaskStatistic(PoolItem<?> poolItem, @Nullable AbstractTask task) {
        this.poolItem = poolItem;
        this.task = task;
    }

}
