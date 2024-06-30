package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerThreadPool;
import ru.jamsys.core.promise.PromiseTask;

@Component
@Lazy
public class ServiceThreadReal {

    public void execute(PromiseTask promiseTask) {
        App.get(ManagerThreadPool.class).addPromiseTask(promiseTask);
    }

}
