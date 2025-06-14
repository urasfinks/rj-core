package ru.jamsys.core.component.cron;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceTimer;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.broker.memory.BrokerMemory;
import ru.jamsys.core.flat.template.cron.release.Cron3s;
import ru.jamsys.core.pool.AbstractPool;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

// Удаление протухших элементов в Manager
// TODO: надо все это сделать на виртуальных потоках, скорость нам не важна
@SuppressWarnings("unused")
@Component
public class Helper3s extends PromiseGenerator implements Cron3s {

    private final ServicePromise servicePromise;

    private final ServiceTimer serviceTimer;

    private final Manager manager;

    public Helper3s() {
        this.servicePromise = App.get(ServicePromise.class);
        this.serviceTimer = App.get(ServiceTimer.class);
        this.manager = App.get(Manager.class);
    }

    @Override
    public Promise generate() {
        return servicePromise.get(App.getUniqueClassName(getClass()), 6_000L)
                .append("manager", (run, _, _) -> manager.helper(run))
                .append("balancePool", (run, _, _) -> manager.groupAcceptByInterface(AbstractPool.class, AbstractPool::balance))
                .append("timer", (threadRun, _, _) -> serviceTimer.helper(threadRun))
                .append(
                        BrokerMemory.class.getSimpleName(),
                        (run, _, _) -> manager.groupAccept(
                                BrokerMemory.class,
                                expirationList -> expirationList.helper(run)
                        )
                )
                ;

    }

}
