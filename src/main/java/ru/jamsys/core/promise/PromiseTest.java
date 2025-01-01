package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class PromiseTest {

    final AbstractPromise abstractPromise;

    public PromiseTest(Promise abstractPromise) {
        this.abstractPromise = (AbstractPromise) abstractPromise;
    }

    public PromiseTest replace(String index, PromiseTask task) {
        PromiseTask[] array = abstractPromise.listPendingTasks.toArray(new PromiseTask[0]);
        abstractPromise.listPendingTasks.clear();
        index = abstractPromise.getComplexIndex(index);
        for (PromiseTask promiseTask : array) {
            if (promiseTask.getIndex().equals(index) && promiseTask.getType().equals(task.getType())) {
                abstractPromise.listPendingTasks.add(task);
            } else {
                abstractPromise.listPendingTasks.add(promiseTask);
            }
        }
        return this;
    }

    public PromiseTest remove(String index) {
        PromiseTask[] array = abstractPromise.listPendingTasks.toArray(new PromiseTask[0]);
        abstractPromise.listPendingTasks.clear();
        index = abstractPromise.getComplexIndex(index);
        for (PromiseTask promiseTask : array) {
            if (!promiseTask.getIndex().equals(index)) {
                abstractPromise.listPendingTasks.add(promiseTask);
            }
        }
        return this;
    }

}
