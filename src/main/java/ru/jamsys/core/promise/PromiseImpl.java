package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.trace.TracePromise;
import ru.jamsys.core.flat.util.Util;

import java.util.List;
import java.util.Set;

public class PromiseImpl extends AbstractPromiseBuilder {

    public static Set<Promise> queueMultipleCompleteSet = Util.getConcurrentHashSet();

    private volatile Thread loopThread;

    public PromiseImpl(String index, long keepAliveOnInactivityMs, long lastActivityMs) {
        super(index, keepAliveOnInactivityMs, lastActivityMs);
    }

    public PromiseImpl(String index, long keepAliveOnInactivityMs) {
        super(index, keepAliveOnInactivityMs);
    }

    @Override
    public void timeOut(String cause) {
        // Timeout может прилетать уже после того, как
        if (isRun.get()) {
            setError("TimeOut cause: " + cause, getExpiredException(), null);
            complete();
        }
    }

    public void complete(@NonNull PromiseTask task, @NonNull Throwable exception) {
        setError(task.getIndex(), exception, task.getType());
        complete();
    }

    public void complete(@Nullable PromiseTask task) {
        if (task != null) {
            setRunningTasks.remove(task);
        }
        complete();
    }

    public void complete() {
        if (isRun.get()) {
            if (isStartLoop.compareAndSet(false, true)) {
                try{
                    loop();
                }catch (Throwable th){
                    // Произошла ошибка, её же никто не обработает
                    setError("loop", th, null);
                    try {
                        loop();
                    } catch (Throwable th2) {
                        App.error(th2);
                    }
                }
                isStartLoop.set(false);
            } else if (!Thread.currentThread().equals(loopThread) && firstConcurrentCompletionWait.compareAndSet(false, true)) {
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
        if (isWait.get() && isNextLoop()) {
            if (setRunningTasks.isEmpty()) {
                isWait.set(false);
                getTrace().add(new TracePromise<>("Все запущенные задачи исполнились. Прекращаем сон. (0)", null, null, null));
            } else {
                return;
            }
        }
        loopThread = Thread.currentThread();
        while (!toHead.isEmpty() && isNextLoop()) {
            List<PromiseTask> promiseTasks = toHead.pollLast();
            if (promiseTasks != null) {
                //listPendingTasks = [4,5]  [x1,x2]
                // -> 1. x2;
                // listPendingTasks = [x2, 4, 5]
                // -> 2. x1
                // listPendingTasks = [x1, x2, 4, 5]
                for (int i = promiseTasks.size() - 1; i >= 0; i--) {
                    PromiseTask promiseTask = promiseTasks.get(i);
                    listPendingTasks.addFirst(promiseTask);
                }
            }
        }
        // Запускаем задачи из pending
        while (!listPendingTasks.isEmpty() && isNextLoop()) {
            if (isWait.get()) {
                if (setRunningTasks.isEmpty()) {
                    isWait.set(false);
                    getTrace().add(new TracePromise<>("Все запущенные задачи исполнились. Прекращаем сон. (1)", null, null, null));
                } else {
                    break;
                }
            }
            PromiseTask firstTask = listPendingTasks.pollFirst();
            if (firstTask != null) {
                switch (firstTask.type) {
                    case WAIT -> {
                        isWait.set(true);
                        getTrace().add(new TracePromise<>("StartWait", null, null, null));
                    }
                    case ASYNC_NO_WAIT_IO, ASYNC_NO_WAIT_COMPUTE -> firstTask.start();
                    default -> {
                        setRunningTasks.add(firstTask);
                        firstTask.start();
                    }
                }
            } else {
                setError("listPendingTasks.peekFirst() return null value", null, null);
            }
        }
        if (isRun.get()) {
            if (isException.get()) {
                isRun.set(false);
                queueMultipleCompleteSet.remove(this);
                if (onError != null) {
                    onError.start();
                }
            } else if (
                    !isWait.get() // Мы не ждём
                            && setRunningTasks.isEmpty() // Список запущенных задач пуст
                            && listPendingTasks.isEmpty() //  Список задач в работу пуст
                            && toHead.isEmpty() // Список добавленных в runTime задача пуст
            ) {
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
                            + "Promise not terminated");
        }
    }

}
