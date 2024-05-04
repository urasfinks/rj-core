package ru.jamsys.core.promise;

import lombok.NonNull;
import org.springframework.lang.Nullable;
import ru.jamsys.core.component.promise.api.PromiseApi;
import ru.jamsys.core.extension.Procedure;
import ru.jamsys.core.statistic.time.TimeControllerMs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface Promise extends TimeControllerMs {

    String getIndex();

    void setIndex(String index);

    void setTimeOut(long ms);

    String getRqUid();

    void complete(@NonNull PromiseTask task, @NonNull Throwable exception);

    void complete(@Nullable PromiseTask task);

    void complete(@Nullable PromiseTask task, List<PromiseTask> toHead);

    void complete();

    boolean inProgress();

    void await(long timeoutMs);

    Promise run();

    Promise setRqUid(String rqUid);

    Promise setLog(boolean log);

    Promise onComplete(Procedure onComplete);

    Promise onError(Consumer<Throwable> onError);

    Promise append(PromiseTask task);

    Promise append(String index, PromiseTaskType promiseTaskType, Function<AtomicBoolean, List<PromiseTask>> fn);

    Promise then(PromiseTask task);

    Promise then(String index, PromiseTaskType promiseTaskType, Function<AtomicBoolean, List<PromiseTask>> fn);

    Promise waits();

    Promise append(String index, PromiseTaskType promiseTaskType, Consumer<AtomicBoolean> fn);

    Promise then(String index, PromiseTaskType promiseTaskType, Consumer<AtomicBoolean> fn);

    PromiseTask getLastAppendedTask();

    List<Trace<String, Throwable>> getExceptionTrace();

    List<Trace<String, TraceTimer>> getTrace();

    String getLog();

    Promise api(String index, PromiseApi<?> promiseApi);

    Map<String, Object> getProperty();

    boolean isCompleted();

}
