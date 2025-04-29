package ru.jamsys.core.pool;

import ru.jamsys.core.App;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// Приватный пул - объекты пула живут собственной жизнью и их нельзя изъять от туда.
// Пул конечно создан, что бы решать внешние задачи.
// Так как все объекты пула живут своей жизнью, они могут пойти поспать или вообще перестать работать
// Для синхронизации с внешним миром - есть звоночек (это как вызов официанта ServiceBell) - что бы взбодрить пул

public abstract class AbstractPoolPrivate<RA, RR, T extends ExpirationMsMutable & Resource<RA, RR>>
        extends AbstractPool<T> {

    public AbstractPoolPrivate(String key) {
        super(key);
    }

    // Звоночек, что бы взбодрить приватные ресурсы
    public void serviceBell() {
        T resource = get();
        if (resource != null) {
            try {
                resource.execute(null);
            } catch (Throwable th) {
                App.error(th);
            }
        }
    }

    @Override
    public void onParkUpdate() {
        // Приватный объект разобрал свои задачи, его оживят когда добавят новые задачи через serviceBell
    }

    @Override
    public boolean forwardResourceWithoutParking(T poolItem) {
        return false;
    }

}
