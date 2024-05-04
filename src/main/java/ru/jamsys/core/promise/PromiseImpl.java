package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class PromiseImpl extends AbstractPromiseBuilder {

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
        if (isStartLoop.compareAndSet(false, true)) {
            loop();
            isStartLoop.set(false);
            // Если накидали в параллель задач, возьмем инициативу повторного проворота
            if (countConcurrentComplete.get() > 0) {
                countConcurrentComplete.decrementAndGet();
                complete();
            }
        } else {
            countConcurrentComplete.incrementAndGet();
            // Обработка частного случая не атомарной операции isStartLoop.set(false);
            if (!isStartLoop.get()) {
                // Вернём добавленный инкремент, что бы сократить кол-во операций
                // Это должно быть именно так - сначала добавить инкремент, что бы дать возможность текущему loop
                // выполнить дополнительную работу и только в том случае, если там уже вышли и высвободили флаг
                // запускать собственный loop с декрементацией счётчика
                countConcurrentComplete.decrementAndGet();
                complete();
            }
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
            if (firstTask.type.isRollback(this)) {
                listPendingTasks.addFirst(firstTask);
                break;
            }
            Thread.onSpinWait();
        }
        if (isRun.get()) {
            if (isException.get()) {
                isRun.set(false);
                if (onError != null) {
                    onError.accept(exception);
                }
            } else if (!inProgress()) {
                isRun.set(false);
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
        long expired = System.currentTimeMillis() + timeoutMs;
        while (inProgress() && expired >= System.currentTimeMillis()) {
            Thread.onSpinWait();
        }
    }

}
