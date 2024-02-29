package ru.jamsys.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.jamsys.thread.ExecutorService;

@Data
@EqualsAndHashCode(callSuper = false)
public class TaskStatisticExecute extends TagIndex {

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

    @Override
    protected String compileIndex() {
        return super.compileIndex() +
                task.getIndex();
    }

    public void finish() {
        timeExecute = System.currentTimeMillis() - timeStart;
    }
}
