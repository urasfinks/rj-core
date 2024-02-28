package ru.jamsys.task;

import lombok.Data;
import ru.jamsys.scheduler.NewThreadWrap;

@Data
public class TaskStatisticExecute {
    NewThreadWrap threadWrap;
    long timeStart = System.currentTimeMillis();
    Long timeExecute = null;
    Task task;

    public TaskStatisticExecute(NewThreadWrap threadWrap, Task task) {
        this.threadWrap = threadWrap;
        this.task = task;
    }

    public void finish() {
        timeExecute = System.currentTimeMillis() - timeStart;
    }
}
