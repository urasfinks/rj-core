package ru.jamsys.core.pool;

import ru.jamsys.core.App;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// Приватный пул - это значит, что объекты этого пула живут собственной жизнью и их нельзя изъять от туда
// Но пул конечно создан, что бы решать внешние задачи
// Так как все объекты пула живут своей жизнью, они могут пойти поспать или вообще перестать работать
// Для синхронизации с внешним миром - есть звоночек (это как вызов официанта ServiceBell) - что бы взбодрить пул

public abstract class AbstractPoolPrivate<RA, RR, PI extends ExpirationMsMutable & Resource<RA, RR>>
        extends AbstractPool<RA, RR, PI> {

    public AbstractPoolPrivate(String name) {
        super(name);
    }

    // Звоночек, что бы взбодрить приватные ресурсы
    public void serviceBell() {
        PI pi = getFromPark();
        if (pi != null) {
            try {
                pi.execute(null);
            } catch (Throwable th) {
                App.error(th);
            }
        }
    }

    @Override
    public void onParkUpdate() {
        // Приватный объект закончил свою работу, нет никакого смысла его снова запускать
    }

    @Override
    public boolean forwardResourceWithoutParking(PI poolItem) {
        return false;
    }

}
