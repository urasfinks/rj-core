package ru.jamsys.core.promise;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import ru.jamsys.core.extension.Procedure;
import ru.jamsys.core.statistic.time.TimeControllerMsImpl;
import ru.jamsys.core.util.UtilJson;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@JsonPropertyOrder({"rqUid", "exception", "completed", "trace", "exceptionTrace", "property"})
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class AbstractPromise extends TimeControllerMsImpl implements Promise {

    @JsonProperty
    @Getter
    protected String rqUid = java.util.UUID.randomUUID().toString();

    protected boolean log = false;

    // Флаг, который говорит, что было конкурентное исполнение
    protected final AtomicInteger countConcurrentComplete = new AtomicInteger(0);

    @JsonProperty
    @Getter
    private final Map<String, Object> property = new HashMap<>();

    protected volatile Throwable exception = null;

    protected final AtomicBoolean isRun = new AtomicBoolean(true);

    protected final AtomicBoolean isException = new AtomicBoolean(false);

    protected final AtomicBoolean isStartLoop = new AtomicBoolean(false);

    // Запущенные задачи, наличие результата, говорит о том, что задача выполнена
    protected final List<PromiseTask> listRunningTasks = new ArrayList<>();

    // Так как задачи могут исполняться в разных потоках, и в момент исполнения могут приходить результаты исполнений
    // будем вставлять в конкурентную очередь, что бы не потерять результат
    protected final ConcurrentLinkedDeque<List<PromiseTask>> toHead = new ConcurrentLinkedDeque<>();

    // Список задач, в голове лежит следующая задача на исполнение
    // Выполненная задача может в голову вставить ещё задач
    // Все действия со стеком происходят в эксклюзивной блокировке
    @Getter
    protected final Deque<PromiseTask> listPendingTasks = new LinkedList<>();

    // Вызовется если все задачи пройдут успешно
    protected Procedure onComplete = null;

    // Вызовется если произошло исключение
    protected Consumer<Throwable> onError = null;

    protected PromiseTaskType lastType = PromiseTaskType.JOIN;

    @JsonProperty("exceptionTrace")
    @Getter
    protected List<Trace<String, Throwable>> exceptionTrace = new ArrayList<>();

    @JsonProperty
    @Getter
    protected List<Trace<String, TraceTimer>> trace = new ArrayList<>();

    @Override
    public void setTimeOut(long ms) {
        setKeepAliveOnInactivityMs(ms);
    }

    @JsonIgnore
    @Override
    public String getLog() {
        return UtilJson.toStringPretty(this, "{}");
    }

    @JsonProperty
    public boolean isCompleted() {
        return !inProgress();
    }

    @JsonProperty
    public boolean isException() {
        return isException.get();
    }

}
