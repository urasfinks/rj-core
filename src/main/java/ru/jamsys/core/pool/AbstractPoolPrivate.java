package ru.jamsys.core.pool;

import ru.jamsys.core.resource.thread.ThreadExecutePromiseTask;

// Приватный пул - объекты пула живут собственной жизнью и их нельзя изъять от туда.
// Пул конечно создан, что бы решать внешние задачи.
// Так как все объекты пула живут своей жизнью, они могут пойти поспать или вообще перестать работать
// Для синхронизации с внешним миром - есть звоночек (это как вызов официанта ServiceBell) - что бы взбодрить пул

public abstract class AbstractPoolPrivate extends AbstractPool<ThreadExecutePromiseTask> {

    public AbstractPoolPrivate(String key) {
        super(key);
    }

    // Звоночек, что бы взбодрить приватные ресурсы
    public void serviceBell() {
        ThreadExecutePromiseTask resource = acquire();
        if (resource != null) {
            resource.execute(null);
        }
    }

    @Override
    public void onParkUpdate() {
        // Приватный объект разобрал свои задачи, его оживят когда добавят новые задачи через serviceBell
    }

    @Override
    public boolean forwardResourceWithoutParking(ThreadExecutePromiseTask poolItem) {
        return false;
    }

}
