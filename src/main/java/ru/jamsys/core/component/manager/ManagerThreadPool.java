package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.resource.thread.ThreadPool;

@Component
public class ManagerThreadPool extends AbstractManager<ThreadPool, Void> {

    public void addPromiseTask(PromiseTask promiseTask) {
        getManagerElement(promiseTask.getIndex(), Void.class, null)
                .addPromiseTask(promiseTask);
    }

    @Override
    public ThreadPool build(String index, Class<?> classItem, Void builderArgument) {
        ThreadPool threadPool = new ThreadPool(index);
        threadPool.run();
        return threadPool;
    }

    @Override
    public int getInitializationIndex() {
        return 5;
    }

}
