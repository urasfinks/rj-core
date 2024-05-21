package ru.jamsys.core.promise;

import ru.jamsys.core.App;
import ru.jamsys.core.component.PromiseTaskTime;
import ru.jamsys.core.promise.resource.api.PromiseApi;

// В бассейне купаются объекты)
// Есть очередь смотрящих, которые приходят к бассейну и ждут, когда им на глаза попадётся объект
// Смотрящий берёт объект на карандаш и пока не надоест смотрит за ним)
// Смотрящий определяет на сколько хорошо плавает объект и либо разрешает ему плавать дальше, либо нахер его из басика
// Желающих понаблюдать за плавающими объектами может быть много, поэтому после ухода первого смотрящего
// Следующий смотрящий в очереди может взять на карандаш этот же объект
// Если за объектом давно никто не наблюдал, мы не понимаем на сколько он норм или нет и выбрасываем его нахер из басика
// Но бывает условие, что минимум в басике должно плавать N объектов, и их не в праве никто выгнать

@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class AbstractPromiseBuilder extends AbstractPromise {

    public AbstractPromiseBuilder(long keepAliveOnInactivityMs, long lastActivityMs) {
        super(keepAliveOnInactivityMs, lastActivityMs);
    }

    public AbstractPromiseBuilder(long keepAliveOnInactivityMs) {
        super(keepAliveOnInactivityMs);
    }

    public Promise setCorrelation(String rqUid) {
        this.correlation = rqUid;
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

    public Promise append(PromiseTaskWithResource<?,?,?> task) {
        listPendingTasks.add(task);
        return this;
    }

    public Promise then(PromiseTask task) {
        this
                .append(new PromiseTask(PromiseTaskExecuteType.WAIT.getName(), this, PromiseTaskExecuteType.WAIT))
                .append(task);
        return this;
    }

    public Promise waits() {
        this.append(new PromiseTask(PromiseTaskExecuteType.WAIT.getName(), this, PromiseTaskExecuteType.WAIT));
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
