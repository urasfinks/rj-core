package ru.jamsys.core.promise;

import ru.jamsys.core.component.promise.api.PromiseApi;
import ru.jamsys.core.extension.Procedure;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class AbstractPromiseBuilder extends AbstractPromise {

    public Promise setRqUid(String rqUid) {
        this.rqUid = rqUid;
        return this;
    }

    public Promise setLog(boolean log) {
        this.log = log;
        return this;
    }

    public Promise setType(PromiseTaskType type) {
        this.lastType = type;
        return this;
    }

    public Promise onComplete(Procedure onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    public Promise onError(Consumer<Throwable> onError) {
        this.onError = onError;
        return this;
    }

    public Promise append(PromiseTask task) {
        setType(task.type);
        listPendingTasks.add(task);
        return this;
    }

    public Promise append(String index, PromiseTaskType promiseTaskType, Function<AtomicBoolean, List<PromiseTask>> fn) {
        append(new PromiseTask(index, this, promiseTaskType, fn));
        return this;
    }

    public Promise append(String index, Function<AtomicBoolean, List<PromiseTask>> fn) {
        append(new PromiseTask(index, this, lastType, fn));
        return this;
    }

    public Promise then(PromiseTask task) {
        this
                .append(new PromiseTask(PromiseTaskType.WAIT.getName(), this, PromiseTaskType.WAIT))
                .append(task);
        return this;
    }

    public Promise then(String index, PromiseTaskType promiseTaskType, Function<AtomicBoolean, List<PromiseTask>> fn) {
        then(new PromiseTask(index, this, promiseTaskType, fn));
        return this;
    }

    public Promise then(String index, Function<AtomicBoolean, List<PromiseTask>> fn) {
        then(new PromiseTask(index, this, lastType, fn));
        return this;
    }

    public Promise waits() {
        this.append(new PromiseTask(PromiseTaskType.WAIT.getName(), this, PromiseTaskType.WAIT));
        return this;
    }

    //-- Builder Producer

    public Promise append(String index, PromiseTaskType promiseTaskType, Consumer<AtomicBoolean> fn) {
        append(new PromiseTask(index, this, promiseTaskType, fn));
        return this;
    }

    public Promise append(String index, Consumer<AtomicBoolean> fn) {
        append(new PromiseTask(index, this, lastType, fn));
        return this;
    }

    public Promise then(String index, PromiseTaskType promiseTaskType, Consumer<AtomicBoolean> fn) {
        then(new PromiseTask(index, this, promiseTaskType, fn));
        return this;
    }

    public Promise then(String index, Consumer<AtomicBoolean> fn) {
        then(new PromiseTask(index, this, lastType, fn));
        return this;
    }

    public Promise run(AtomicBoolean isThreadRun) {
        complete();
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
