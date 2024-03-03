package ru.jamsys.thread;

import ru.jamsys.App;
import ru.jamsys.component.Broker;
import ru.jamsys.task.TaskHandlerStatistic;
import ru.jamsys.util.Util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractExecutorService implements ExecutorService{

    private final AtomicBoolean isRun = new AtomicBoolean(false);
    protected final AtomicBoolean isWhile = new AtomicBoolean(false);
    protected final Queue<Thread> listThread = new ConcurrentLinkedDeque<>();

    @Override
    public void reload() {
        shutdown();
        run();
    }

    public boolean run() {
        return isRun.compareAndSet(false, true);
    }

    public boolean shutdown() { //timeMaxExecute 1500+1500 = 3000ms
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
            Util.riskModifierCollection(null, listThread, new Thread[0], (Thread thread) -> {
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
            Broker broker = App.context.getBean(Broker.class);
            while (!listThread.isEmpty()) { //Сгружаем оставшиеся потоки
                Thread thread = listThread.poll();
                if (thread != null) {
                    Util.logConsole("Thread " + thread.getName() + " > stop()");
                    ru.jamsys.broker.Queue<TaskHandlerStatistic> taskHandlerStatisticQueue = broker.get(TaskHandlerStatistic.class);
                    Util.riskModifierCollection(
                            null,
                            taskHandlerStatisticQueue.getCloneQueue(null),
                            new TaskHandlerStatistic[0],
                            (TaskHandlerStatistic taskStatisticExecute) -> {
                                if (taskStatisticExecute.getThread().equals(thread)) {
                                    taskHandlerStatisticQueue.remove(taskStatisticExecute);
                                }
                            });
                    thread.stop(); //Ну как бы всё, извините, на этом мои полномочия всё
                }
            }
            isRun.set(false);
            return true;
        }
        return false;
    }
}
