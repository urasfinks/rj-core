package ru.jamsys.core.component.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ClassFinderComponent;
import ru.jamsys.core.component.PromiseComponent;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ManagerCheckReserved implements Cron1s, PromiseGenerator, ClassName {

    private final List<AbstractManager> list = new ArrayList<>();

    private final String index;

    private final PromiseComponent promiseComponent;

    public ManagerCheckReserved(ClassFinderComponent classFinderComponent, ApplicationContext applicationContext, PromiseComponent promiseComponent) {
        this.promiseComponent = promiseComponent;
        index = getClassName("cron", applicationContext);
        classFinderComponent.findByInstance(AbstractManager.class).forEach(managerClass
                -> list.add(applicationContext.getBean(managerClass)));
    }

    @Override
    public Promise generate() {
        return promiseComponent.get(index, 6_000L)
                .append("CheckReserved", (AtomicBoolean _, Promise _)
                        -> list.forEach(AbstractManager::checkReserved));
    }
}