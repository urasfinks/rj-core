package ru.jamsys.core.promise;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.Procedure;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableImpl;
import ru.jamsys.core.util.UtilJson;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@JsonPropertyOrder({"rqUid", "index", "addTime", "expTime", "diffTimeMs", "exception", "completed", "trace", "exceptionTrace", "property"})
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class AbstractPromise extends ExpiredMsMutableImpl implements Promise {

    @JsonProperty
    @Setter
    @Getter
    private String index;

    @JsonProperty
    @Getter
    protected String rqUid = java.util.UUID.randomUUID().toString();

    protected boolean log = false;

    protected AtomicBoolean waiters = new AtomicBoolean(false);

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

    @JsonProperty
    public String getAddTime() {
        return getLastActivityFormat();
    }

    @JsonProperty
    public String getExpTime() { //Сократил, что бы время InitTime было ровно над временем ExprTime
        return getExpiredFormat();
    }

    @JsonProperty
    public long getDiffTimeMs() { //Сократил, что бы время InitTime было ровно над временем ExprTime
        return getExpiryRemainingMs();
    }

}
