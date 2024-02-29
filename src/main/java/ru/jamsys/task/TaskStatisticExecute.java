package ru.jamsys.task;

import lombok.Data;
import ru.jamsys.thread.ExecutorService;

@Data
public class TaskStatisticExecute {

    final Thread thread;
    final ExecutorService executorService;
    long timeStart = System.currentTimeMillis();
    Long timeExecute = null;
    Task task;

    public TaskStatisticExecute(ExecutorService executorService, Thread thread, Task task) {
        this.executorService = executorService;
        this.thread = thread;
        this.task = task;
    }

    public void finish() {
        timeExecute = System.currentTimeMillis() - timeStart;
    }
}
