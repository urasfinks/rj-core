package ru.jamsys.scheduler;

import lombok.Data;
import ru.jamsys.Util;
import ru.jamsys.task.Task;
import ru.jamsys.task.TaskStatisticExecute;
import ru.jamsys.task.TimeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class NewThread {

    private ExecutorService executorService = null;
    private final long delay;
    private final AtomicBoolean isRun = new AtomicBoolean(false);
    private NewThreadWrap currentThread = new NewThreadWrap();
    private List<Task> listTask = new ArrayList<>();
    private TimeManager timeManager = new TimeManager();

    public NewThread(long delay) {
        this.delay = delay;
    }

    public void run() {
        if (isRun.compareAndSet(false, true)) {
            executorService = Executors.newFixedThreadPool(1);
            executorService.submit(() -> {
                currentThread.setThread(Thread.currentThread());
                currentThread.setNewThread(this);
                long nextStart = System.currentTimeMillis();
                while (isRun.get()) {
                    nextStart = Util.zeroLastNDigits(nextStart + delay, 3);
                    getListTask().forEach(task -> {
                        TaskStatisticExecute taskStatisticExecute = new TaskStatisticExecute(currentThread, task);
                        timeManager.getQueue().add(taskStatisticExecute);
                        task.getConsumer().accept(isRun);
                        taskStatisticExecute.finish();
                    });
                    if (!currentThread.getThread().isInterrupted() && isRun.get()) {
                        long calcSleep = nextStart - System.currentTimeMillis();
                        if (calcSleep > 0) {
                            Util.sleepMillis(calcSleep);
                        } else {
                            Util.sleepMillis(1);//Что бы поймать Interrupt
                            nextStart = System.currentTimeMillis();
                        }
                    } else {
                        break;
                    }
                }
                System.out.println(currentThread.getThread() + ": STOP");
            });
        }
    }

    public void reload() {
        shutdown();
        Util.sleepMillis(1000);
        isRun.set(true);
        run();
    }

    public void shutdown() {
        if (executorService != null) {
            isRun.set(false);
            if (currentThread.getThread() != null) {
                currentThread.getThread().interrupt();
            }
            executorService.shutdown();
            executorService = null;
        }
    }
}
