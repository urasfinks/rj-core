package ru.jamsys.core.pool;

import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

// Приватный пул - это значит, что объекты этого пула живут собственной жизнью и их нельзя изъять от туда
// Но пул конечно создан, что бы решать внешние задачи
// Так как все объекты пула живут своей жизнью, они могут пойти поспать или вообще перестать работать
// Для синхронизации с внешним миром - есть звоночек (это как вызов официанта ServiceBell) - что бы взбодрить пул

public abstract class AbstractPoolPrivate<RA, RR, PI extends ExpirationMsMutable & Resource<Void, RA, RR>>
        extends AbstractPool<Void, RA, RR, PI> {

    public AbstractPoolPrivate(String name, Class<PI> cls) {
        super(name, cls);
    }

    // Звоночек, что бы взбодрить приватные ресурсы
    public void serviceBell() {

    }

    @Override
    public void onParkUpdate() {
        // Приватный объект закончил свою работу, нет никакого смысла его снова запускать
    }
}
