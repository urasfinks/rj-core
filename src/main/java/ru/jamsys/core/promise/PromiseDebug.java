package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.NonNull;
import ru.jamsys.core.extension.trace.Trace;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PromiseDebug implements Promise {

    private final Promise promise;

    @Getter
    private final PromiseTask promiseTask;

    private PromiseRepositoryDebug promiseRepositoryDebug;

    public PromiseDebug(Promise promise, PromiseTask promiseTask) {
        this.promise = promise;
        this.promiseTask = promiseTask;
    }

    @Override
    public String getIndex() {
        return promise.getIndex();
    }

    @Override
    public void complete(@NonNull PromiseTask task, @NonNull Throwable exception) {
        promise.complete(task, exception);
    }

    @Override
    public void complete(PromiseTask task) {
        promise.complete(task);
    }

    @Override
    public void complete() {
        promise.complete();
    }

    @Override
    public void await(long timeoutMs) {
        promise.await(timeoutMs);
    }

    @Override
    public Promise run() {
        return promise.run();
    }

    @Override
    public Promise onComplete(PromiseTask onComplete) {
        return promise.onComplete(onComplete);
    }

    @Override
    public boolean isSetErrorHandler() {
        return promise.isSetErrorHandler();
    }

    @Override
    public boolean isSetCompleteHandler() {
        return promise.isSetCompleteHandler();
    }

    @Override
    public Promise onError(PromiseTask onError) {
        return promise.onError(onError);
    }

    @Override
    public void addToHead(List<PromiseTask> append) {
        promise.addToHead(append);
    }

    @Override
    public Promise append(PromiseTask task) {
        return promise.append(task);
    }

    @Override
    public PromiseTask getLastTask() {
        return promise.getLastTask();
    }

    @Override
    public List<Trace<String, Throwable>> getExceptionTrace() {
        return promise.getExceptionTrace();
    }

    @Override
    public Collection<Trace<String, ?>> getTrace() {
        return promise.getTrace();
    }

    @Override
    public String getLogString() {
        return promise.getLogString();
    }

    @Override
    public boolean isRun() {
        return promise.isRun();
    }

    @Override
    public boolean isException() {
        return promise.isException();
    }

    @Override
    public void timeOut(String cause) {
        promise.timeOut(cause);
    }

    @Override
    public Throwable getException() {
        return promise.getException();
    }

    @Override
    public void setError(String index, Throwable throwable) {
        promise.setError(index, throwable);
    }

    @Override
    public void setErrorInRunTask(Throwable throwable) {
        promise.setErrorInRunTask(throwable);
    }

    @Override
    public Promise setLog(boolean log) {
        return promise.setLog(log);
    }

    @Override
    public void skipAllStep() {
        promise.skipAllStep();
    }

    @Override
    public void goTo(String to) {
        promise.goTo(to);
    }

    @Override
    public Promise setDebug(boolean debug) {
        return promise.setDebug(debug);
    }

    @Override
    public boolean isDebug() {
        return promise.isDebug();
    }

    @Override
    public Map<String, Object> getRepositoryMapWithoutDebug() {
        return promise.getRepositoryMapWithoutDebug();
    }

    @Override
    public String getCorrelation() {
        return promise.getCorrelation();
    }

    @Override
    public void setCorrelation(String correlation) {
        promise.setCorrelation(correlation);
    }

    @Override
    public Map<String, Object> getRepositoryMap() {
        if (promise.isDebug()) {
            if (promiseRepositoryDebug == null) {
                promiseRepositoryDebug = new PromiseRepositoryDebug(promise.getRepositoryMapWithoutDebug(), this);
            }
            return promiseRepositoryDebug;
        }
        return promise.getRepositoryMap();
    }

    @Override
    public long getLastActivityMs() {
        return promise.getLastActivityMs();
    }

    @Override
    public long getKeepAliveOnInactivityMs() {
        return promise.getKeepAliveOnInactivityMs();
    }

    @Override
    public void setStopTimeMs(Long timeMs) {
        promise.setStopTimeMs(timeMs);
    }

    @Override
    public Long getStopTimeMs() {
        return promise.getStopTimeMs();
    }

}