package ru.jamsys.core.component.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.ArrayList;
import java.util.List;

// Восстановления законсервированных элементов AbstractManager при наличии их повторной активности

@SuppressWarnings("unused")
@Component
public class ReservedHelper implements Cron1s, PromiseGenerator, UniqueClassName {

    private final List<AbstractManager> list = new ArrayList<>();

    private final ServicePromise servicePromise;

    public ReservedHelper(ServiceClassFinder serviceClassFinder, ApplicationContext applicationContext, ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
        serviceClassFinder.findByInstance(AbstractManager.class).forEach(managerClass
                -> list.add(applicationContext.getBean(managerClass)));
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 6_000L)
                .append("main", (_, _, _) -> list.forEach(AbstractManager::checkReserved));
    }
}
