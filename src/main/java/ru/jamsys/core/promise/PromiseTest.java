package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

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

    public PromiseTest removeAfter(String index) {
        PromiseTask[] array = abstractPromise.listPendingTasks.toArray(new PromiseTask[0]);
        abstractPromise.listPendingTasks.clear();
        index = abstractPromise.getComplexIndex(index);
        PromiseTask removeTask = null;
        for (PromiseTask promiseTask : array) {
            if (promiseTask.getIndex().equals(index)) {
                removeTask = promiseTask;
            }
        }
        if (removeTask != null) {
            boolean remove = false;
            for (PromiseTask promiseTask : array) {
                if (!remove) {
                    abstractPromise.listPendingTasks.add(promiseTask);
                }
                if (promiseTask.equals(removeTask)) {
                    remove = true;
                }
            }
        }
        return this;
    }

    public PromiseTest removeBefore(String index) {
        PromiseTask[] array = abstractPromise.listPendingTasks.toArray(new PromiseTask[0]);
        abstractPromise.listPendingTasks.clear();
        index = abstractPromise.getComplexIndex(index);
        boolean remove = true;
        for (PromiseTask promiseTask : array) {
            if (promiseTask.getIndex().equals(index)) {
                System.out.println(index);
                remove = false;
            }
            if (!remove) {
                abstractPromise.listPendingTasks.add(promiseTask);
            }
        }
        return this;
    }

    public List<String> getIndex() {
        List<String> result = new ArrayList<>();
        PromiseTask[] array = abstractPromise.listPendingTasks.toArray(new PromiseTask[0]);
        for (PromiseTask promiseTask : array) {
            result.add(
                    promiseTask.getIndex().substring(abstractPromise.getIndex().length() + 1)
                            + "::" + promiseTask.getType().toString()
            );
        }
        return result;
    }

}
