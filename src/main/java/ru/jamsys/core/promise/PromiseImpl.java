package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class PromiseImpl extends AbstractPromiseBuilder {

    public static ConcurrentLinkedDeque<Promise> queueMultipleComplete = new ConcurrentLinkedDeque<>();

    public PromiseImpl(String index, long keepAliveOnInactivityMs, long lastActivityMs) {
        super(keepAliveOnInactivityMs, lastActivityMs);
        setIndex(index);
    }

    public PromiseImpl(String index, long keepAliveOnInactivityMs) {
        super(keepAliveOnInactivityMs);
        setIndex(index);
    }

    @Override
    public void expired() {
        setError("Expired", new RuntimeException("Expired"));
        complete();
    }

    private void setError(String indexTask, Throwable exception) {
        this.exception = exception;
        this.exceptionTrace.add(new Trace<>(indexTask, exception));
        isException.set(true);
    }

    public void complete(@NonNull PromiseTask task, @NonNull Throwable exception) {
        setError(task.getIndex(), exception);
        complete();
    }

    public void complete(@Nullable PromiseTask task) {
        if (task != null) {
            task.setComplete(true);
        }
        complete();
    }

    public void complete(@Nullable PromiseTask task, List<PromiseTask> toHead) {
        if (task != null) {
            task.setComplete(true);
        }
        if (toHead != null) {
            this.toHead.add(toHead);
        }
        complete();
    }

    public void complete() {
        if (isRun.get()) {
            if (isStartLoop.compareAndSet(false, true)) {
                loop();
                isStartLoop.set(false);
            } else if (firstConcurrentCompletionWait.compareAndSet(false, true)) {
                long expiredTimeMs = System.currentTimeMillis() + 5;
                while (isStartLoop.get()) {
                    if (System.currentTimeMillis() > expiredTimeMs) {
                        break;
                    }
                    Thread.onSpinWait();
                }
                firstConcurrentCompletionWait.set(false);
                if (!isStartLoop.get()) {
                    complete();
                }
            } else if (!queueMultipleComplete.contains(this)) {
                queueMultipleComplete.add(this);
            }
        } else {
            //TODO: тут наверное надо повторно откинуть лог, допустим сюда могут исполненные задачи, но время закончилось
        }
    }

    private boolean isNextLoop() {
        if (isException.get()) {
            return false;
        }
        if (isExpired()) {
            setError(getClass() + ".isNextLoop()", getExpiredException());
            return false;
        }
        return true;
    }

    private void loop() {
        while (!toHead.isEmpty() && isNextLoop()) {
            List<PromiseTask> promiseTasks = toHead.pollLast();
            assert promiseTasks != null;
            for (int i = promiseTasks.size() - 1; i >= 0; i--) {
                listPendingTasks.addFirst(promiseTasks.get(i));
            }
        }
        while (!listPendingTasks.isEmpty() && isNextLoop()) {
            PromiseTask firstTask = listPendingTasks.pollFirst();
            assert firstTask != null;
            firstTask.start();
            if (firstTask.type.isRunnable()) { //Так мы откинули WAIT
                listRunningTasks.add(firstTask);
            }
            if (firstTask.type == PromiseTaskType.WAIT) {
                if (isRunningTaskNotComplete()) {
                    listPendingTasks.addFirst(firstTask);
                    break;
                }
            }
            Thread.onSpinWait();
        }
        if (isRun.get()) {
            if (isException.get()) {
                isRun.set(false);
                queueMultipleComplete.remove(this);
                if (onError != null) {
                    onError.accept(exception);
                }
            } else if (!inProgress()) {
                isRun.set(false);
                queueMultipleComplete.remove(this);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }
    }

    // Список задач не пуст или запущенные задачи ещё не выполнены
    public boolean inProgress() {
        if (isException.get()) {
            return false;
        }
        if (!listPendingTasks.isEmpty()) {
            return true;
        }
        return isRunningTaskNotComplete();
    }

    // Не все запущенные задачи имеют результат
    public boolean isRunningTaskNotComplete() {
        //Что бы не словить модификатор при вызове из другого потока, так как публичный метод
        Object[] objects = listRunningTasks.toArray();
        for (Object t : objects) {
            PromiseTask tx = (PromiseTask) t;
            if (!tx.isComplete()) {
                return true;
            }
        }
        return false;
    }

    public void await(long timeoutMs) {
        long expiredTime = System.currentTimeMillis() + timeoutMs;
        while (inProgress() && expiredTime >= System.currentTimeMillis()) {
            Thread.onSpinWait();
        }
    }

}
