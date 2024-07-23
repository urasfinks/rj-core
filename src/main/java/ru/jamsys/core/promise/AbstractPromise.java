package ru.jamsys.core.promise;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.Correlation;
import ru.jamsys.core.extension.trace.TracePromise;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableImpl;
import ru.jamsys.core.statistic.timer.nano.TimerNanoEnvelope;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@JsonPropertyOrder({"correlation", "index", "addTime", "expTime", "diffTimeMs", "exception", "completed", "trace", "exceptionTrace", "property"})
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class AbstractPromise extends ExpirationMsImmutableImpl implements Promise, Correlation {

    @JsonIgnore
    @Getter
    private boolean log = false;

    @JsonProperty
    @Getter
    private final String index;

    @Setter
    @Getter
    protected String correlation = java.util.UUID.randomUUID().toString();

    // Первый поток, который зашёл одновременно с выполнением основного loop будет пробовать ждать 5мс
    // Что бы перехватить инициативу крутить основной loop
    protected AtomicBoolean firstConcurrentCompletionWait = new AtomicBoolean(false);

    @JsonIgnore
    @Getter
    private final Map<String, Object> mapRepository = new ConcurrentHashMap<>();

    @Getter
    protected volatile Throwable exception = null;

    protected final AtomicBoolean isRun = new AtomicBoolean(false);

    protected final AtomicBoolean isWait = new AtomicBoolean(false);

    protected final AtomicBoolean isException = new AtomicBoolean(false);

    protected final AtomicBoolean isStartLoop = new AtomicBoolean(false);

    // Запущенные задачи, наличие результата, говорит о том, что задача выполнена
    protected final Set<PromiseTask> setRunningTasks = Util.getConcurrentHashSet();

    // Так как задачи могут исполняться в разных потоках, и в момент исполнения могут приходить результаты исполнений
    // будем вставлять в конкурентную очередь, что бы не потерять результат
    protected final ConcurrentLinkedDeque<List<PromiseTask>> toHead = new ConcurrentLinkedDeque<>();

    // Список задач, в голове лежит следующая задача на исполнение
    // Выполненная задача может в голову вставить ещё задач
    // Все действия со стеком происходят в эксклюзивной блокировке
    @Getter
    protected final Deque<PromiseTask> listPendingTasks = new ConcurrentLinkedDeque<>();

    // Вызовется если все задачи пройдут успешно
    protected PromiseTask onComplete = null;

    // Вызовется если произошло исключение
    protected PromiseTask onError = null;

    @JsonProperty("exceptionTrace")
    @Getter
    protected List<TracePromise<String, Throwable>> exceptionTrace = new ArrayList<>();

    @JsonProperty
    @Getter
    protected Collection<TracePromise<String, TimerNanoEnvelope<String>>> trace = new ConcurrentLinkedQueue<>();

    public AbstractPromise(String index, long keepAliveOnInactivityMs, long lastActivityMs) {
        super(keepAliveOnInactivityMs, lastActivityMs);
        this.index = index;
    }

    public AbstractPromise(String index, long keepAliveOnInactivityMs) {
        super(keepAliveOnInactivityMs);
        this.index = index;
    }

    @JsonProperty
    public boolean isRun() {
        return isRun.get();
    }

    public AbstractPromise setLog(boolean log){
        this.log = log;
        return this;
    }

    @JsonIgnore
    @Override
    public String getLogString() {
        return UtilJson.toStringPretty(this, "{}");
    }

    public void addToHead(List<PromiseTask> append) {
        toHead.add(append);
    }

    @JsonProperty
    @Override
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
        return getInactivityTimeMs();
    }

    protected void setError(String indexTask, Throwable exception, PromiseTaskExecuteType type) {
        this.exception = exception;
        this.exceptionTrace.add(new TracePromise<>(indexTask, exception, type, null));
        isException.set(true);
    }

    public void setErrorInRunTask(Throwable throwable) {
        this.exception = throwable;
        isException.set(true);
    }

}
