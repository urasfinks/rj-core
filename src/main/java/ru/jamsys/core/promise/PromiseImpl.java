package ru.jamsys.core.promise;

import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceLoggerRemote;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class PromiseImpl extends AbstractPromiseBuilder {

    private volatile Thread loopThread;

    private final AtomicBoolean terminateAction = new AtomicBoolean(false);

    private final ConcurrentLinkedDeque<String> goTo = new ConcurrentLinkedDeque<>();

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
        if (run.get()) {
            setError(new ForwardException("TimeOut cause: " + cause, genExpiredException()));
            completePromise();
        }
    }

    public void completePromise(@NonNull PromiseTask task, @NonNull Throwable exception) {
        setError(task, exception);
        completePromise();
    }

    public void completePromise(@Nullable PromiseTask task) {
        if (task != null) {
            setRunningTasks.remove(task);
        }
        completePromise();
    }

    public void completePromise() {
        if (run.get()) {
            if (startLoop.compareAndSet(false, true)) {
                try {
                    loop();
                } catch (Throwable th) {
                    setError(new ForwardException("loop", th));
                    App.error(getExceptionSource());
                    // Произошла ошибка, её же никто не обработает
                    // Самостоятельно сделаем ещё попытку выполнить задачи, в надежде что вызовется терминальный блок
                    try {
                        loop();
                    } catch (Throwable th2) {
                        setError(new ForwardException("loop-retry", th));
                        App.error(getExceptionSource());
                    }
                }
                startLoop.set(false);
            } else if (
                // Проверяем, что это не мы сами, защита от поедания хвоста змеи - самое змеёй
                    !Thread.currentThread().equals(loopThread)
                            // Получаем уникальный доступ на возможность немного подождать исполнение
                            && firstWaiting.compareAndSet(false, true)
            ) {
                Util.await(startLoop, 5L, null);
                firstWaiting.set(false);
                if (!startLoop.get()) {
                    completePromise();
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
        if (exception.get()) {
            return false;
        }
        if (isExpired()) {
            setError(new ForwardException("isNextLoop", genExpiredException()));
            return false;
        }
        return true;
    }

    private void loop() {
        if (wait.get() && isNextLoop()) {
            if (setRunningTasks.isEmpty()) {
                // Все запущенные задачи исполнились
                wait.set(false);
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
            if (wait.get()) {
                if (setRunningTasks.isEmpty()) {
                    // Все запущенные задачи исполнились
                    wait.set(false);
                } else {
                    break;
                }
            }
            PromiseTask poolTask = listPendingTasks.pollFirst();
            if (poolTask != null) {
                // Если сейчас команда не "ожидание" и выставлен goTo
                // А если ожидание - то будем сначала ждать, а только потом прыгать
                if (poolTask.type != PromiseTaskExecuteType.WAIT && !goTo.isEmpty()) {
                    // Сначала проверяем, а случаем эта задача не та самая на которую надо скакнуть
                    // Если нет, то просто логируем, что пропускаем задачу и проворачиваем while
                    if (!goTo.peekFirst().equals(poolTask.getIndex())) {
                        getTrace().add(new Trace<>("Skip task: " + poolTask.getIndex() + "; goTo: " + goTo.peek(), null));
                        continue;
                    } else { // Если задача та самая == goTo, то вычленяем из коллекции и идём дальше на исполнение
                        goTo.pollFirst();
                    }
                }
                switch (poolTask.type) {
                    case WAIT -> {
                        getTrace().add(new Trace<>("Wait (" + poolTask.getIndex() + ")", null));
                        wait.set(true);
                    }
                    case ASYNC_NO_WAIT_IO, ASYNC_NO_WAIT_COMPUTE -> poolTask.prepareLaunch(null);
                    default -> {
                        setRunningTasks.add(poolTask);
                        poolTask.prepareLaunch(null);
                    }
                }
            } else {
                // Встречал такую проблему, когда запускается skipAllStep(String cause)
                setError(new RuntimeException("listPendingTasks.peekFirst() return null value"));
            }
        }
        if (run.get()) {
            if (exception.get()) {
                terminate(onError);
                if (isPrintError()) {
                    UtilLog.printError(getClass(), getLogString());
                }
            } else if (
                    !wait.get() // Мы не ждём
                            && setRunningTasks.isEmpty() // Список запущенных задач пуст
                            && listPendingTasks.isEmpty() //  Список задач в работу пуст
                            && toHead.isEmpty() // Список добавленных в runTime задача пуст
            ) {
                if (!goTo.isEmpty()) {
                    setError(new RuntimeException("goTo is not empty: " + goTo));
                    terminate(onError);
                    if (isPrintError()) {
                        UtilLog.printError(getClass(), getLogString());
                    }
                } else {
                    terminate(onComplete);
                }
            }
        }
    }

    private void terminate(PromiseTask promiseTask) {
        if (terminateAction.compareAndSet(false, true)) {
            App.get(ServicePromise.class).finish(registerInBroker);
            ServicePromise.queueMultipleCompleteSet.remove(this);
            stop();
            if (promiseTask != null) {
                promiseTask.prepareLaunch(() -> run.set(false));
                // Дальнейшие действия под капотом
                // 1) установится isRun.set(false)
                // 2) Вызовится complete
                // 3) complete пойдёт по ветке flushLog()
                //
                // Тут есть проблема, что лог будет зафиксирован после того, как закончится await
                // Пока не знаю на сколько это плохо
            } else {
                run.set(false);
                flushLog();
            }
        }
    }

    @Override
    public void skipAllStep(String cause) {
        getTrace().add(new Trace<>("skipAllStep " + cause, null));
        listPendingTasks.clear();
    }

    @Override
    public void goTo(String to) {
        getTrace().add(new Trace<>("goTo " + to, null));
        goTo.add(getIndex() + "." + to);
    }

    private void flushLog() {
        String logString = null;
        if (isDebug()) {
            logString = getLogString();
            UtilLog.printInfo(getClass(), logString);
        }
        if (isLog() && App.get(ServiceLoggerRemote.class).getRemote()) {
            if (logString == null) {
                logString = getLogString();
            }
            if (exception.get()) {
                UtilLog.error(getClass(), logString).sendRemote();
            } else {
                UtilLog.info(getClass(), logString).sendRemote();
            }
        }
    }

    public Promise await(long timeoutMs) {
        Util.await(run, timeoutMs, "Promise not terminated");
        return this;
    }

    @Override
    public Promise await(long timeoutMs, int sleepIterationMs) {
        Util.await(run, timeoutMs, sleepIterationMs, "Promise not terminated");
        return this;
    }

}
