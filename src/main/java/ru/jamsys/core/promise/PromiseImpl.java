package ru.jamsys.core.promise;

import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceLogger;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.component.manager.item.LogType;
import ru.jamsys.core.extension.trace.TracePromise;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PromiseImpl extends AbstractPromiseBuilder {

    private volatile Thread loopThread;

    private final AtomicBoolean finalAction = new AtomicBoolean(false);

    @Setter
    private DisposableExpirationMsImmutableEnvelope<Promise> registerInBroker;

    @SuppressWarnings("unused")
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

    @Override
    public void skipAllStep() {
        listPendingTasks.clear();
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
                    App.error(th);
                    setError("loop", th, null);
                    try {
                        loop();
                    } catch (Throwable th2) {
                        App.error(th2);
                    }
                }
                isStartLoop.set(false);
            } else if (!Thread.currentThread().equals(loopThread) && firstConcurrentCompletionWait.compareAndSet(false, true)) {
                Util.await(isStartLoop, 5, null);
                firstConcurrentCompletionWait.set(false);
                if (!isStartLoop.get()) {
                    complete();
                }
            } else {
                ServicePromise.queueMultipleCompleteSet.add(this);
            }
        } else {
            // Этот блок может отработать, если какая-то задача выполнялась дольше чем сработал timeout promise
            // Задача в трейсах должна зафиксировать время своего исполнения и нам для общего понимания
            // полагаю пригодится эта информация
            flushLog();
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
                getTrace().add(new TracePromise<>(Thread.currentThread().getName() + " Все запущенные задачи исполнились. Прекращаем сон. (0)", null, null, null));
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
                    getTrace().add(new TracePromise<>(Thread.currentThread().getName() + " Все запущенные задачи исполнились. Прекращаем сон. (1)", null, null, null));
                } else {
                    break;
                }
            }
            PromiseTask poolTask = listPendingTasks.pollFirst();
            if (poolTask != null) {
                switch (poolTask.type) {
                    case WAIT -> {
                        isWait.set(true);
                        getTrace().add(new TracePromise<>("StartWait", null, null, null));
                    }
                    case ASYNC_NO_WAIT_IO, ASYNC_NO_WAIT_COMPUTE -> poolTask.start(null);
                    default -> {
                        setRunningTasks.add(poolTask);
                        poolTask.start(null);
                    }
                }
            } else {
                setError("listPendingTasks.peekFirst() return null value", null, null);
            }
        }
        if (isRun.get()) {
            if (isException.get()) {
                if (finalAction.compareAndSet(false, true)) {
                    ServicePromise.queueMultipleCompleteSet.remove(this);
                    App.get(ServicePromise.class).finish(registerInBroker);
                    if (onError != null) {
                        onError.start(() -> isRun.set(false));
                    } else {
                        isRun.set(false);
                        flushLog();
                    }
                }
            } else if (
                    !isWait.get() // Мы не ждём
                            && setRunningTasks.isEmpty() // Список запущенных задач пуст
                            && listPendingTasks.isEmpty() //  Список задач в работу пуст
                            && toHead.isEmpty() // Список добавленных в runTime задача пуст
            ) {
                if (finalAction.compareAndSet(false, true)) {
                    ServicePromise.queueMultipleCompleteSet.remove(this);
                    App.get(ServicePromise.class).finish(registerInBroker);
                    if (onComplete != null) {
                        onComplete.start(() -> isRun.set(false));
                        // Дальнейшие действия под капотом
                        // 1) установится isRun.set(false)
                        // 2) Вызовится complete
                        // 3) complete пойдёт по ветке flushLog()
                        //
                        // Тут есть проблема, что лог будет зафиксирован после того, как закончится await
                        // Пока не знаю на сколько это плохо
                    } else {
                        isRun.set(false);
                        flushLog();
                    }
                }
            }
        }
    }

    private void flushLog() {
        if (isLog()) {
            App.get(ServiceLogger.class).add(new Log(
                    isException.get() ? LogType.ERROR : LogType.INFO,
                    getCorrelation()
            ).setData(getLogString()));
        }
    }

    public void await(long timeoutMs) {
        Util.await(isRun, timeoutMs, "Promise not terminated");
    }

}
