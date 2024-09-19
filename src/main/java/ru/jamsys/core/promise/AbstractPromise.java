package ru.jamsys.core.promise;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.Correlation;
import ru.jamsys.core.extension.exception.PromiseException;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@JsonPropertyOrder({"correlation", "index", "addTime", "expTime", "stopTime", "diffTimeMs", "exception", "completed", "trace", "exceptionTrace", "property"})
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
    protected AtomicBoolean firstWaiting = new AtomicBoolean(false);

    @JsonIgnore
    private final Map<String, Object> repositoryMap = new ConcurrentHashMap<>();

    @Getter
    protected volatile Throwable exception = null;

    protected final AtomicBoolean run = new AtomicBoolean(false);

    protected final AtomicBoolean wait = new AtomicBoolean(false);

    protected final AtomicBoolean isException = new AtomicBoolean(false);

    protected final AtomicBoolean startLoop = new AtomicBoolean(false);

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
    private final List<Trace<String, Throwable>> exceptionTrace = new ArrayList<>();

    @JsonProperty
    @Getter
    private final Collection<Trace<String, ?>> trace = new ConcurrentLinkedQueue<>();

    @Getter
    private boolean debug;

    public Promise setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

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
        return run.get();
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

    @JsonProperty
    public String getStopTime() { //Сократил, что бы время InitTime было ровно над временем ExprTime
        return getStopFormat();
    }

    private void setErrorNative(PromiseException throwable) {
        this.exception = throwable;
        isException.set(true);
    }

    public void setError(Throwable throwable) {
        if (throwable == null) {
            throwable = new RuntimeException("Throwable is null");
        }
        this.exceptionTrace.add(new Trace<>(null, throwable));
        setErrorNative(new PromiseException(this, null, throwable));
    }

    public void setError(PromiseTask promiseTask, Throwable throwable) {
        promiseTask.getTracePromiseTask().getExceptionTrace().add(new Trace<>(null, throwable));
        setErrorNative(new PromiseException(this, promiseTask, throwable));
    }

    // Это для extension, когда ещё promise не запущен, но уже ведутся работа с репозиторием
    // И как бы надо логировать, если включен debug
    public Map<String, Object> getRepositoryMap() {
        return isDebug() ? new PromiseRepositoryDebug(repositoryMap, this) : repositoryMap;
    }

    // Специально помечаю как упразднённый, что бы не было соблазна его использовать
    // На самом деле он никогда не будет удалён, просто это чисто системный метод, для решения отладочной информации
    @Deprecated
    public Map<String, Object> getRepositoryMapWithoutDebug() {
        return repositoryMap;
    }

}
