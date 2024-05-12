package ru.jamsys.core.promise;

import ru.jamsys.core.App;
import ru.jamsys.core.component.promise.PromiseTaskTime;
import ru.jamsys.core.component.promise.api.PromiseApi;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class AbstractPromiseBuilder extends AbstractPromise {

    public AbstractPromiseBuilder(long keepAliveOnInactivityMs, long lastActivityMs) {
        super(keepAliveOnInactivityMs, lastActivityMs);
    }

    public AbstractPromiseBuilder(long keepAliveOnInactivityMs) {
        super(keepAliveOnInactivityMs);
    }

    public Promise setRqUid(String rqUid) {
        this.rqUid = rqUid;
        return this;
    }

    public Promise setLog(boolean log) {
        this.log = log;
        return this;
    }

    public Promise onComplete(PromiseTask onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    public Promise onError(PromiseTask onError) {
        this.onError = onError;
        return this;
    }

    public Promise append(PromiseTask task) {
        listPendingTasks.add(task);
        return this;
    }

    public Promise then(PromiseTask task) {
        this
                .append(new PromiseTask(PromiseTaskType.WAIT.getName(), this, PromiseTaskType.WAIT))
                .append(task);
        return this;
    }

    public Promise waits() {
        this.append(new PromiseTask(PromiseTaskType.WAIT.getName(), this, PromiseTaskType.WAIT));
        return this;
    }

    //-- Builder Producer

    public Promise run() {
        complete();
        if (onError != null) {
            App.context.getBean(PromiseTaskTime.class).addExpiration(this);
        }
        return this;
    }

    public PromiseTask getLastAppendedTask() {
        return listPendingTasks.peekLast();
    }

    @Override
    public Promise api(String index, PromiseApi<?> promiseApi) {
        promiseApi.setIndex(index);
        promiseApi.extend(this);
        return this;
    }

}
