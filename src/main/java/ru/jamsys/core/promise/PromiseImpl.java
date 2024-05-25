package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.flat.util.Util;

import java.util.List;
import java.util.Set;

public class PromiseImpl extends AbstractPromiseBuilder {

    public static Set<Promise> queueMultipleCompleteSet = Util.getConcurrentHashSet();

    public PromiseImpl(String index, long keepAliveOnInactivityMs, long lastActivityMs) {
        super(keepAliveOnInactivityMs, lastActivityMs);
        setIndex(index);
    }

    public PromiseImpl(String index, long keepAliveOnInactivityMs) {
        super(keepAliveOnInactivityMs);
        setIndex(index);
    }

    @Override
    public void timeOut(String cause) {
        setError("TimeOut cause: " + cause, getExpiredException(), null);
        complete();
    }

    private void setError(String indexTask, Throwable exception, PromiseTaskExecuteType type) {
        this.exception = exception;
        this.exceptionTrace.add(new Trace<>(indexTask, exception, type, null));
        isException.set(true);
    }

    public void complete(@NonNull PromiseTask task, @NonNull Throwable exception) {
        setError(task.getIndex(), exception, task.getType());
        complete();
    }

    public void complete(@Nullable PromiseTask task) {
        if (task != null) {
//            int before = setRunningTasks.size();

            setRunningTasks.remove(task);
            countCompleteTask.incrementAndGet();
//            if (getIndex().equals("test")) {
//                System.out.println("REMOVE1: " + task + "; sizeBefore: " + before + " sizeAfter: " + setRunningTasks.size());
//            }
        }
        //complete();
    }

    public void complete(@Nullable PromiseTask task, List<PromiseTask> toHead) {
        if (task != null) {
            int before = setRunningTasks.size();
            setRunningTasks.remove(task);
            countCompleteTask.incrementAndGet();
//            if (getIndex().equals("test")) {
//                System.out.println("REMOVE2: " + task + "; sizeBefore: " + before + " sizeAfter: " + setRunningTasks.size());
//            }
        }
        if (toHead != null) {
            this.toHead.add(toHead);
        }
        complete();
    }

    public void complete() {
//        if (getIndex().equals("test")) {
//            Util.printStackTrace("loop " + getIndex() + "; isRun: " + isRun.get());
//        }
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
            } else {
                queueMultipleCompleteSet.add(this);
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
            setError("TimeOut.onNextLoop()", getExpiredException(), null);
            return false;
        }
        return true;
    }

    private void loop() {
        while (!toHead.isEmpty() && isNextLoop()) {
            List<PromiseTask> promiseTasks = toHead.pollLast();
            assert promiseTasks != null;
            for (int i = promiseTasks.size() - 1; i >= 0; i--) {
                PromiseTask promiseTask = promiseTasks.get(i);
                listPendingTasks.addFirst(promiseTask);
                if (promiseTask.type.isRunningTask()) {
                    countRunnableTask.incrementAndGet();
                }
            }
        }
        // Запускаем задачи из pending
        while (!listPendingTasks.isEmpty() && isNextLoop()) {
            PromiseTask firstTask = listPendingTasks.pollFirst();
            assert firstTask != null;
//            if (getIndex().equals("test")) {
//                System.out.println("start task:" + firstTask.getIndex());
//            }
            if (firstTask.type.isRunningTask()) { //Так мы откинули WAIT
                setRunningTasks.add(firstTask);
            }
            firstTask.start();
//            if (getIndex().equals("test")) {
//                System.out.println("finish start task:" + firstTask.getIndex());
//            }
            if (firstTask.type == PromiseTaskExecuteType.WAIT) {
                if (!setRunningTasks.isEmpty()) {
//                    if (getIndex().equals("test")) {
//                        System.out.println("reinsert wait because setRunningTasks.size() = " + setRunningTasks);
//                    }
                    listPendingTasks.addFirst(firstTask);
                    break;
                }
            }
            Thread.onSpinWait();
        }
        if (isRun.get()) {
            if (isException.get()) {
                isRun.set(false);
                queueMultipleCompleteSet.remove(this);
                if (onError != null) {
                    onError.start();
                }
            } else if (isTerminated()) {
                isRun.set(false);
                queueMultipleCompleteSet.remove(this);
                if (onComplete != null) {
                    onComplete.start();
                }
            }
        }
    }

    public void await(long timeoutMs) {
        long start = System.currentTimeMillis();
        long expiredTime = start + timeoutMs;
        while (!isTerminated() && expiredTime >= System.currentTimeMillis()) {
            Thread.onSpinWait();
        }
        if (!isTerminated()) {
            Util.printStackTrace(
                    "await timeout start: " + Util.msToDataFormat(start)
                            + " now: " + Util.msToDataFormat(System.currentTimeMillis()) + ";\r\n"
                            + getAny());
        }
    }

    public String getAny() {
        return " isTerminated: " + isTerminated() + ";\n"
                + " countRunnableTask: " + countRunnableTask.get() + ";\n"
                + " countCompleteTask: " + countCompleteTask.get() + ";\n"
                + " isException.get(): " + isException.get() + ";";
    }

}
