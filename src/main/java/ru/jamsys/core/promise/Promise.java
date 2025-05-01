package ru.jamsys.core.promise;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.LogType;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ConsumerThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskConsumerThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskWithResourceConsumerThrowing;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.resource.PoolResourcePromiseTaskWaitResource;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceConfiguration;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// Цепочка обещаний
@JsonPropertyOrder({"correlation", "index", "addTime", "expTime", "stopTime", "diffTimeMs", "exception", "trace", "property"})
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@Getter
public class Promise extends ExpirationMsImmutableImpl implements RepositoryMapClass<Object> {

    private final String index;

    @Setter
    @Accessors(chain = true)
    private LogType logType;

    private TerminalStatus terminalStatus = TerminalStatus.IN_PROCESS;

    private final AtomicBoolean run = new AtomicBoolean(false);

    // Очередь задач
    @JsonIgnore
    private final WaitQueue<AbstractPromiseTask> queueTask = new WaitQueue<>();

    @JsonProperty
    private final Collection<Trace<String, ?>> trace = new ConcurrentLinkedQueue<>();

    // Контекст между AbstractPromiseTask
    @JsonIgnore
    private final Map<String, Object> repositoryMap = new ConcurrentHashMap<>();

    // Добавление задачи, которая выполнится после фатального завершения цепочки Promise.
    // Вызовется если произошло исключение
    @Setter
    @Accessors(chain = true)
    private AbstractPromiseTask onError = null;

    private final AtomicBoolean errorRun = new AtomicBoolean(false); // что бы двойных вызовов не было

    // Добавление задачи, которая выполнится после успешного завершения цепочки Promise.
    // Вызовется если все задачи пройдут успешно
    @Setter
    @Accessors(chain = true)
    private AbstractPromiseTask onComplete = null;

    private final AtomicBoolean completeRun = new AtomicBoolean(false); // что бы двойных вызовов не было

    // Для DEBUG режима кешируем репозиторий отладки
    private PromiseRepositoryDebug promiseRepositoryDebug;

    @SuppressWarnings("all")
    @Setter
    DisposableExpirationMsImmutableEnvelope<Promise> registeredTimeOutExpiration;

    public Promise(String index, long keepAliveOnInactivityMs, long lastActivityMs) {
        super(keepAliveOnInactivityMs, lastActivityMs);
        this.index = index;
    }

    public Promise(String index, long keepAliveOnInactivityMs) {
        super(keepAliveOnInactivityMs);
        this.index = index;
    }

    public void completePromiseTask(AbstractPromiseTask task) {
        if (task != null) {
            trace.add(new Trace<>(task.getNs() + ".complete()", null));
        }
        List<AbstractPromiseTask> promiseTasks = queueTask.commitAndPoll(task);
        if (!promiseTasks.isEmpty()) {
            for (AbstractPromiseTask promiseTask : promiseTasks) {
                promiseTask.prepareLaunch(null);
            }
        } else if (queueTask.isTerminal()) {
            if (App.get(ServicePromise.class).removeTimeout(registeredTimeOutExpiration)) {
                runCompleteHandler();
            } else {
                trace.add(new Trace<>("::ServicePromise.removeTimeOut(false)", null));
                runErrorHandler();
            }
        }
    }

    public void runCompleteHandler() {
        if (!isRun()) {
            return;
        }
        if (hasCompleteHandler()) {
            if (completeRun.compareAndSet(false, true)) {
                onComplete.prepareLaunch(() -> {
                    terminalStatus = TerminalStatus.SUCCESS;
                    run.set(false);
                });
            }
        } else {
            terminalStatus = TerminalStatus.SUCCESS;
            run.set(false);
        }
    }

    public void runErrorHandler() {
        if (!isRun()) {
            return;
        }
        if (hasErrorHandler()) {
            if (errorRun.compareAndSet(false, true)) {
                onError.prepareLaunch(() -> run.set(false));
            }
        } else {
            run.set(false);
        }
    }

    public void timeOut(String cause) {
        // Timeout может прилетать уже после того, как
        if (isRun()) {
            setError("::timeOut", new ForwardException(cause, genExpiredException()));
        }
    }

    public void skipAllStep(AbstractPromiseTask promiseTask, String cause) {
        trace.add(new Trace<>(promiseTask.getNs() + ".skipAllStep(" + cause + ")", null));
        queueTask.skipAll();
    }

    public void goTo(AbstractPromiseTask promiseTask, String toIndexTask) {
        trace.add(new Trace<>(promiseTask.getNs() + ".goTo(" + toIndexTask + ")", null));
        queueTask.skipUntil(toIndexTask);
    }

    public Promise append(AbstractPromiseTask task) {
        queueTask.getMainQueue().add(task);
        return this;
    }

    // Это для extension, когда ещё promise не запущен, но уже ведутся работа с репозиторием
    // И как бы надо логировать, если включен debug
    public Map<String, Object> getRepositoryMap() {
        if (logType == LogType.DEBUG) {
            if (promiseRepositoryDebug == null) {
                promiseRepositoryDebug = new PromiseRepositoryDebug(repositoryMap);
            }
            return promiseRepositoryDebug;
        }
        return repositoryMap;
    }

    // Запускаем цепочку задач от текущего потока
    public Promise run() {
        trace.add(new Trace<>(index + "::run()", null));
        run.set(true);
        terminalStatus = TerminalStatus.IN_PROCESS;
        completePromiseTask(null);
        return this;
    }

    @JsonProperty
    public boolean isRun() {
        return run.get();
    }

    // Блок должен вызываться 1 раз. Может быть такое, что 10 параллельно запущенных задач смогут вызвать 10 setError
    public void setError(String index, Throwable throwable) {
        this.trace.add(new Trace<>(index, throwable));
        terminalStatus = TerminalStatus.ERROR;
        runErrorHandler();
    }

    // Проверка, что добавлен обработчик ошибки
    public boolean hasErrorHandler() {
        return this.onError != null;
    }

    // Проверка, что добавлен обработчик успешного выполнения
    public boolean hasCompleteHandler() {
        return this.onComplete != null;
    }

    public Promise onComplete(PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> fn) {
        AbstractPromiseTask promiseTask = createTaskCompute("::CompleteTask", fn);
        return setOnComplete(promiseTask);
    }

    public Promise onError(PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> fn) {
        AbstractPromiseTask promiseTask = createTaskCompute("::ErrorTask", fn);
        return setOnError(promiseTask);
    }

    public Promise append(List<AbstractPromiseTask> listPromiseTask) {
        listPromiseTask.forEach(this::append);
        return this;
    }

    public Promise append(String index, PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> fn) {
        return append(createTaskCompute(index, fn));
    }

    public Promise modifyLastPromiseTask(Consumer<AbstractPromiseTask> fn) {
        fn.accept(queueTask.getMainQueue().peekLast());
        return this;
    }

    public <T extends Resource<?, ?>> Promise appendWithResource(
            String index,
            Class<T> classResource,
            PromiseTaskWithResourceConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise, T> procedure
    ) {
        return appendWithResource(index, classResource, "default", procedure);
    }

    public <T extends Resource<?, ?>> Promise appendWithResource(
            String index,
            Class<T> classResource,
            String ns,
            PromiseTaskWithResourceConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise, T> procedure
    ) {
        return append(createTaskResource(index, classResource, ns, procedure));
    }

    public <T extends Resource<?, ?>> Promise thenWithResource(
            String index,
            Class<T> classResource,
            PromiseTaskWithResourceConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise, T> procedure
    ) {
        return thenWithResource(index, classResource, "default", procedure);
    }

    public <T extends Resource<?, ?>> Promise thenWithResource(
            String index,
            Class<T> classResource,
            String ns,
            PromiseTaskWithResourceConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise, T> procedure
    ) {
        return then(createTaskResource(index, classResource, ns, procedure));
    }

    public AbstractPromiseTask promiseToTask(String index, @NonNull Promise externalPromise) {
        this.setRepositoryMapClass(Promise.class, index, externalPromise);
        return createTaskExternal(
                index,
                (_, promiseTask, promise) -> externalPromise
                        .onError((_, _, externalPromise1) -> {
                            promise.setError(promiseTask.getNs(), new RuntimeException("ExternalPromiseException"));
                            promise.getTrace().addAll(externalPromise1.getTrace());
                        })
                        .onComplete((_, _, _) -> promise.completePromiseTask(promiseTask))
                        .run()
        );
    }

    public Promise then(String index, Promise externalPromise) {
        if (externalPromise == null) {
            return this;
        }
        append(new PromiseTaskWait(getComplexIndex(index), this));
        append(promiseToTask(index, externalPromise));
        return this;
    }

    public Promise append(String index, Promise externalPromise) {
        if (externalPromise == null) {
            return this;
        }
        append(promiseToTask(index, externalPromise));
        return this;
    }

    public Promise then(AbstractPromiseTask task) {
        appendWait(task.getNs()).append(task);
        return this;
    }

    public Promise then(String index, PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> fn) {
        return then(createTaskCompute(index, fn));
    }

    public Promise appendWait() {
        append(new PromiseTaskWait(this));
        return this;
    }

    public Promise appendWait(String key) {
        append(new PromiseTaskWait(key, this));
        return this;
    }

    // Для удобства использования builder, что бы в цепочке можно было навешать promise плюшек
    public Promise extension(ConsumerThrowing<Promise> cs) {
        try {
            cs.accept(this);
        } catch (Throwable th) {
            throw new ForwardException(th);
        }
        return this;
    }

    public String getComplexIndex(String index) {
        return getComplexIndex(getIndex(), index);
    }

    public static String getComplexIndex(String promiseIndex, String promiseTaskIndex) {
        return promiseIndex + CascadeKey.append(promiseTaskIndex);
    }

    public AbstractPromiseTask createTaskCompute(String index, PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> fn) {
        return new PromiseTask(getComplexIndex(index), this, PromiseTaskExecuteType.COMPUTE, fn);
    }

    public AbstractPromiseTask createTaskIo(String index, PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> fn) {
        return new PromiseTask(getComplexIndex(index), this, PromiseTaskExecuteType.IO, fn);
    }

    public AbstractPromiseTask createTaskExternal(String index, PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> fn) {
        return new PromiseTask(getComplexIndex(index), this, PromiseTaskExecuteType.ASYNC_COMPUTE, fn);
    }

    public AbstractPromiseTask createTaskWait(String index) {
        return new PromiseTaskWait(index, this);
    }

    public <T extends Resource<?, ?>> AbstractPromiseTask createTaskResource(
            String index,
            Class<T> classResource,
            String ns,
            PromiseTaskWithResourceConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise, T> procedure
    ) {
        Manager.Configuration<PoolResourcePromiseTaskWaitResource> poolResourcePromiseTaskWaitResourceConfiguration = App.get(Manager.class).configure(
                PoolResourcePromiseTaskWaitResource.class,
                ns,
                ns1 -> new PoolResourcePromiseTaskWaitResource<>(
                        ns1,
                        ns2 -> {
                            T bean = App.context.getBean(classResource);
                            try {
                                bean.init(new ResourceConfiguration(ns2));
                            } catch (Throwable th) {
                                throw new ForwardException(th);
                            }
                            return bean;
                        }
                )
        );
        return new PromiseTaskWaitResource<>(
                getComplexIndex(index),
                this,
                procedure,
                poolResourcePromiseTaskWaitResourceConfiguration
        );
    }

    public <T extends Resource<?, ?>> AbstractPromiseTask createTaskResource(
            String index,
            Class<T> classResource,
            PromiseTaskWithResourceConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise, T> procedure
    ) {
        return createTaskResource(index, classResource, "default", procedure);
    }

    // Синхронное ожидание выполнения Promise
    public Promise await(long timeoutMs) {
        Util.await(getRun(), timeoutMs, "await(" + timeoutMs + ") -> Promise not terminated");
        return this;
    }

    // Синхронное ожидание выполнения Promise
    public Promise await(long timeoutMs, int sleepIterationMs) {
        Util.await(getRun(), timeoutMs, sleepIterationMs, "await(" + timeoutMs + ", " + sleepIterationMs + ") -> Promise not terminated");
        return this;
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

    public enum TerminalStatus {
        SUCCESS,
        ERROR,
        IN_PROCESS
    }

}
