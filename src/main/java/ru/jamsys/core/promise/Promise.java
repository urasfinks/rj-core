package ru.jamsys.core.promise;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.LogType;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ConsumerThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskConsumerThrowing;
import ru.jamsys.core.extension.functional.PromiseTaskWithResourceConsumerThrowing;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.resource.PoolResourcePromiseTaskWaitResource;
import ru.jamsys.core.resource.Resource;
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
//@JsonPropertyOrder({"index", "addTime", "expTime", "stopTime", "diffTimeMs", "exception", "trace", "property"})
//@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@Getter
public class Promise extends ExpirationMsImmutableImpl implements RepositoryMapClass<Object> {

    private final String ns;

    @Setter
    @Accessors(chain = true)
    private LogType logType;

    private TerminalStatus terminalStatus = TerminalStatus.IN_PROCESS;

    private final AtomicBoolean run = new AtomicBoolean(false);

    // Очередь задач
    private final WaitQueue<AbstractPromiseTask> queueTask = new WaitQueue<>();

    private final Collection<Trace<String, ?>> trace = new ConcurrentLinkedQueue<>();

    // Контекст между AbstractPromiseTask
    private final Map<String, Object> repositoryMap = new ConcurrentHashMap<>();

    private final AtomicBoolean terminalRun = new AtomicBoolean(false); // что бы двойных вызовов не было

    // Добавление задачи, которая выполнится после фатального завершения цепочки Promise.
    // Вызовется если произошло исключение
    @Setter
    @Accessors(chain = true)
    private AbstractPromiseTask onError = null;

    // Добавление задачи, которая выполнится после успешного завершения цепочки Promise.
    // Вызовется если все задачи пройдут успешно
    @Setter
    @Accessors(chain = true)
    private AbstractPromiseTask onComplete = null;

    // Для DEBUG режима кешируем репозиторий отладки
    private PromiseRepositoryDebug promiseRepositoryDebug;

    @SuppressWarnings("all")
    @Setter
    private DisposableExpirationMsImmutableEnvelope<Promise> registeredTimeOutExpiration;

    public Promise(String ns, long keepAliveOnInactivityMs, long lastActivityMs) {
        super(keepAliveOnInactivityMs, lastActivityMs);
        this.ns = ns;
    }

    public Promise(String ns, long keepAliveOnInactivityMs) {
        super(keepAliveOnInactivityMs);
        this.ns = ns;
    }

    public void completePromiseTask(AbstractPromiseTask task) {
        // Promise.run() запускает completePromiseTask(null), что бы не дублировать логику commitAndPoll
        if (task != null) {
            trace.add(new Trace<>(task.getNs() + "::complete()", null));
        }
        // Если обещание не запущено, допустим если уже отработал setError. Не надо уже ничего добавлять и обрабатывать
        // Если используется PersistBroker для логирование, состояние уже сброшено на диск и его не дополнить и не
        // переписать
        if (!isRun()) {
            return;
        }
        // Если время promise истекло
        if (this.isExpired()) {
            // Удаляем timeout, так как сами сейчас его запустим, без ожидания общего планировщика
            App.get(ServicePromise.class).removeTimeout(registeredTimeOutExpiration);
            timeOut();
            return;
        }
        // Закомитим таску, что она выполнена и получим новую пачку задач, конечно в том случае если все задачи до WAIT
        // были исполнены (эта логика внутри WaitQueue)
        List<AbstractPromiseTask> promiseTasks = queueTask.commitAndPoll(task);
        if (!promiseTasks.isEmpty()) {
            for (AbstractPromiseTask promiseTask : promiseTasks) {
                promiseTask.prepareLaunch(null);
            }
        } else if (queueTask.isTerminal()) {
            if (App.get(ServicePromise.class).removeTimeout(registeredTimeOutExpiration)) {
                runCompleteHandler();
            } else {
                trace.add(new Trace<>(getNs() + "::removeTimeOut()->false->::runErrorHandler()", null));
                runErrorHandler();
            }
        }
    }

    public void runCompleteHandler() {
        if (!isRun()) {
            return;
        }
        if (hasCompleteHandler()) {
            if (terminalRun.compareAndSet(false, true)) {
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
            if (terminalRun.compareAndSet(false, true)) {
                onError.prepareLaunch(() -> run.set(false));
            }
        } else {
            run.set(false);
        }
    }

    public void timeOut() {
        if (isRun()) {
            setError(getNs() + "::timeOut()", genExpiredException());
        }
    }

    public void skipAllStep(AbstractPromiseTask promiseTask, String cause) {
        trace.add(new Trace<>(promiseTask.getNs() + "::skipAllStep(" + cause + ")", null));
        queueTask.skipAll();
    }

    public void skipUntil(AbstractPromiseTask promiseTask, String toNsTask) {
        String title = promiseTask.getNs() + "::goTo(" + toNsTask + ")";
        if (queueTask.skipUntil(getComplexIndex(toNsTask))) {
            trace.add(new Trace<>(title, null));
        } else {
            setError(title, new RuntimeException("Not found"));
        }
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
        trace.add(new Trace<>(ns + "::run()", null));
        run.set(true);
        terminalStatus = TerminalStatus.IN_PROCESS;
        completePromiseTask(null);
        return this;
    }

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
        AbstractPromiseTask promiseTask = createTaskCompute("onComplete", fn);
        return setOnComplete(promiseTask);
    }

    public Promise onError(PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> fn) {
        AbstractPromiseTask promiseTask = createTaskCompute("onError", fn);
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

    @SuppressWarnings("all")
    public Promise appendWait() {
        if (queueTask.getMainQueue().isEmpty()) {
            return this;
        }
        if(queueTask.getMainQueue().peekLast().isWait()){
            return this;
        }
        append(new PromiseTaskWait(this));
        return this;
    }

    public Promise appendWait(String key) {
        if (queueTask.getMainQueue().isEmpty()) {
            return this;
        }
        if(queueTask.getMainQueue().peekLast().isWait()){
            return this;
        }
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
        return getComplexIndex(getNs(), index);
    }

    public static String getComplexIndex(String promiseIndex, String promiseTaskIndex) {
        return promiseIndex + CascadeKey.append(promiseTaskIndex);
    }

    public AbstractPromiseTask createTaskCompute(String index, PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> fn) {
        return new PromiseTask(getComplexIndex(index), this, PromiseTaskExecuteType.COMPUTE, fn);
    }

    @SuppressWarnings("unused")
    public AbstractPromiseTask createTaskIo(String index, PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> fn) {
        return new PromiseTask(getComplexIndex(index), this, PromiseTaskExecuteType.IO, fn);
    }

    public AbstractPromiseTask createTaskExternal(String index, PromiseTaskConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise> fn) {
        return new PromiseTask(getComplexIndex(index), this, PromiseTaskExecuteType.ASYNC_COMPUTE, fn);
    }

    @SuppressWarnings("unused")
    public AbstractPromiseTask createTaskWait(String index) {
        return new PromiseTaskWait(index, this);
    }

    public <T extends Resource<?, ?>> AbstractPromiseTask createTaskResource(
            String index,
            Class<T> classResource,
            String ns,
            PromiseTaskWithResourceConsumerThrowing<AtomicBoolean, AbstractPromiseTask, Promise, T> procedure
    ) {
        @SuppressWarnings("all")
        Manager.Configuration<PoolResourcePromiseTaskWaitResource> poolResourcePromiseTaskWaitResourceConfiguration = App.get(Manager.class).configure(
                PoolResourcePromiseTaskWaitResource.class,
                ns,
                ns1 -> new PoolResourcePromiseTaskWaitResource<>(
                        ns1,
                        ns2 -> {
                            T bean = App.context.getBean(classResource);
                            try {
                                bean.init(ns2);
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

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    // Синхронное ожидание выполнения Promise
    public Promise await(long timeoutMs, int sleepIterationMs) {
        Util.await(getRun(), timeoutMs, sleepIterationMs, "await(" + timeoutMs + ", " + sleepIterationMs + ") -> Promise not terminated");
        return this;
    }

    public enum TerminalStatus {
        SUCCESS,
        ERROR,
        IN_PROCESS
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("addTime", getLastActivityFormat())
                .append("expiration", getExpiredFormat())
                .append("diffTime", getInactivityTimeMs())
                .append("ns", ns)
                .append("run", run.get())
                .append("terminalStatus", terminalStatus)
                .append("trace", trace)
                ;
    }

}
