package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.resource.thread.ThreadPoolPromiseTask;
import ru.jamsys.core.resource.thread.ThreadResourcePromiseTask;

@Component
public class ManagerThreadPoolPromiseTask extends AbstractManager<ThreadPoolPromiseTask, Void> implements CascadeKey {

    public void addPromiseTask(PromiseTask promiseTask) {
        getManagerElement(promiseTask.getIndex(), ThreadResourcePromiseTask.class, null)
                .addPromiseTask(promiseTask);
    }

    @Override
    public ThreadPoolPromiseTask build(String key, Class<?> classItem, Void builderArgument) {
        ThreadPoolPromiseTask threadPoolPromiseTask = new ThreadPoolPromiseTask(getCascadeKey(key, classItem));
        threadPoolPromiseTask.run();
        return threadPoolPromiseTask;
    }

    @Override
    public int getInitializationIndex() {
        return 5;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeKey getParentCascadeKey() {
        return App.cascadeName;
    }

    public RateLimit getRateLimit(String index) {
        return getManagerElement(index, Void.class, null).getRateLimit();
    }

}
