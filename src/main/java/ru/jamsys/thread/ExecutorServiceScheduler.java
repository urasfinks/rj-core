package ru.jamsys.thread;

import lombok.Data;
import ru.jamsys.Util;
import ru.jamsys.component.Broker;
import ru.jamsys.task.Task;
import ru.jamsys.task.TaskStatisticExecute;
import ru.jamsys.task.TimeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class ExecutorServiceScheduler implements ExecutorService {

    private java.util.concurrent.ExecutorService executorService;
    private final Queue<Thread> listThread = new ConcurrentLinkedDeque<>();
    private final long delay;
    private final AtomicBoolean isRun = new AtomicBoolean(false);
    private final AtomicBoolean isWhile = new AtomicBoolean(false);

    private List<Task> listTask = new ArrayList<>();
    private TimeManager timeManager = new TimeManager();
    private Broker broker;

    public ExecutorServiceScheduler(long delay) {
        this.delay = delay;
    }

    public void run() {
        if (isRun.compareAndSet(false, true)) {
            executorService = Executors.newSingleThreadExecutor(new CustomThreadFactory(getClass().getSimpleName()));
            executorService.submit(() -> {
                isWhile.set(true);
                Thread currentThread = Thread.currentThread();
                listThread.add(currentThread);
                long nextStart = System.currentTimeMillis();
                while (isWhile.get() && !currentThread.isInterrupted()) {
                    nextStart = Util.zeroLastNDigits(nextStart + delay, 3);
                    getListTask().forEach(task -> {
                        TaskStatisticExecute taskStatisticExecute = new TaskStatisticExecute(this, currentThread, task);
                        timeManager.getQueue().add(taskStatisticExecute);
                        try {
                            task.getConsumer().accept(isWhile);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        taskStatisticExecute.finish();
                    });
                    if (isWhile.get()) {
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
                listThread.remove(currentThread);
                System.out.println(currentThread.getName() + ": STOP");
            });
        }
    }

    public void reload() {
        shutdown();
        run();
    }

    public void shutdown() { //timeMaxExecute 1500+1500 = 3000ms
        if (isRun.get()) { //ЧТо бы что-то тушит, надо что бы это что-то было поднято)
            isWhile.set(false); //Говорим всем потокам и их внутренним циклам, что пора заканчивать
            long timeOutMillis = 1500;
            long startTime = System.currentTimeMillis();
            while (!listThread.isEmpty()) { //Пытаемся подождать пока потоки самостоятельно закончат свою работу
                Util.sleepMillis(timeOutMillis / 3);
                if (System.currentTimeMillis() - startTime > timeOutMillis) { //Не смогли за отведённое время
                    Util.logConsole(getClass().getSimpleName() + " Self-stop timeOut shutdown " + timeOutMillis + " ms");
                    break;
                }
            }
            Util.riskModifierCollection(listThread, new Thread[0], (Thread thread) -> {
                Util.logConsole("Thread " + thread.getName() + " > interrupt()");
                thread.interrupt();
            });
            startTime = System.currentTimeMillis();
            while (!listThread.isEmpty()) { //Пытаемся подождать пока потоки выйдут от interrupt
                Util.sleepMillis(timeOutMillis / 3);
                if (System.currentTimeMillis() - startTime > timeOutMillis) { //Не смогли за отведённое время
                    Util.logConsole(getClass().getSimpleName() + " interrupt timeOut shutdown " + timeOutMillis + " ms");
                    break;
                }
            }
            while (!listThread.isEmpty()) { //Сгружаем оставшиеся потоки
                Thread poll = listThread.poll();
                if (poll != null) {
                    Util.logConsole("Thread " + poll.getName() + " > stop()");
                    Util.riskModifierCollection(timeManager.getQueue(), new TaskStatisticExecute[0], (TaskStatisticExecute taskStatisticExecute) -> {
                        if (taskStatisticExecute.getThread().equals(poll)) {
                            //TODO: закинуть на обработку onKill для Task
                            timeManager.getQueue().remove(taskStatisticExecute);
                        }
                    });
                    poll.stop(); //Ну как бы всё, извините, на этом мои полномочия всё
                }
            }
            isRun.set(false);
            executorService.shutdown();
        }
    }
}
