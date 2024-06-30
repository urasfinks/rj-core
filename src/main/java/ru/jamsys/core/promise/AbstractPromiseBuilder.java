package ru.jamsys.core.promise;

// В бассейне купаются объекты)
// Есть очередь смотрящих, которые приходят к бассейну и ждут, когда им на глаза попадётся объект
// Смотрящий берёт объект на карандаш и пока не надоест смотрит за ним)
// Смотрящий определяет на сколько хорошо плавает объект и либо разрешает ему плавать дальше, либо нахер его из басика
// Желающих понаблюдать за плавающими объектами может быть много, поэтому после ухода первого смотрящего
// Следующий смотрящий в очереди может взять на карандаш этот же объект
// Если за объектом давно никто не наблюдал, мы не понимаем на сколько он норм или нет и выбрасываем его нахер из басика
// Но бывает условие, что минимум в басике должно плавать N объектов, и их не в праве никто выгнать

public abstract class AbstractPromiseBuilder extends AbstractPromise {

    public AbstractPromiseBuilder(String index, long keepAliveOnInactivityMs, long lastActivityMs) {
        super(index, keepAliveOnInactivityMs, lastActivityMs);
    }

    public AbstractPromiseBuilder(String index, long keepAliveOnInactivityMs) {
        super(index, keepAliveOnInactivityMs);
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
        if (task == null) {
            throw new RuntimeException("Promise.append() task is null");
        }
        if (isRun.get()) {
            throw new RuntimeException("Promise.append() before run(); index: " + task.getIndex());
        }
        listPendingTasks.add(task);
        return this;
    }

    public Promise then(PromiseTask task) {
        this
                .appendWait()
                .append(task);
        return this;
    }

    public Promise appendWait() {
        this.append(new PromiseTask(PromiseTaskExecuteType.WAIT.getName(), this, PromiseTaskExecuteType.WAIT));
        return this;
    }

    //-- Builder Producer

    public Promise run() {
        isRun.set(true);
        complete();
        return this;
    }

    public PromiseTask getLastTask() {
        return listPendingTasks.peekLast();
    }

}
