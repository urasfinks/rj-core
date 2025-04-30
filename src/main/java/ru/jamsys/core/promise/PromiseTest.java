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

    final Promise abstractPromise;

    public PromiseTest(Promise abstractPromise) {
        this.abstractPromise = abstractPromise;
    }

    public PromiseTest replace(String index, AbstractPromiseTask task) {
        AbstractPromiseTask[] array = abstractPromise.getQueueTask().getMainQueue().toArray(new AbstractPromiseTask[0]);
        abstractPromise.getQueueTask().getMainQueue().clear();
        index = abstractPromise.getComplexIndex(index);
        for (AbstractPromiseTask promiseTask : array) {
            // Столкнулся с проблемой, что хочу поменять для тестов тип задачи
            // ранее была promiseTaskWithResource::IO, но для тестов мне это избыточно
            // мне не нужна БД, поэтому комментирую проверку типа замены
            // Но проверка типа была, что бы WAIT не поменять, так что меняем всё кроме WAIT
            if (
                    promiseTask.getNamespace().equals(index)
                            && !promiseTask.getType().equals(PromiseTaskExecuteType.WAIT)
                //&& promiseTask.getType().equals(task.getType())
            ) {
                abstractPromise.getQueueTask().getMainQueue().add(task);
            } else {
                abstractPromise.getQueueTask().getMainQueue().add(promiseTask);
            }
        }
        return this;
    }

    public PromiseTest remove(String index) {
        AbstractPromiseTask[] array = abstractPromise.getQueueTask().getMainQueue().toArray(new AbstractPromiseTask[0]);
        abstractPromise.getQueueTask().getMainQueue().clear();
        index = abstractPromise.getComplexIndex(index);
        for (AbstractPromiseTask promiseTask : array) {
            if (!promiseTask.getNamespace().equals(index)) {
                abstractPromise.getQueueTask().getMainQueue().add(promiseTask);
            }
        }
        return this;
    }

    public PromiseTest removeAfter(String index) {
        AbstractPromiseTask[] array = abstractPromise.getQueueTask().getMainQueue().toArray(new AbstractPromiseTask[0]);
        abstractPromise.getQueueTask().getMainQueue().clear();
        index = abstractPromise.getComplexIndex(index);
        AbstractPromiseTask removeTask = null;
        for (AbstractPromiseTask promiseTask : array) {
            if (promiseTask.getNamespace().equals(index)) {
                removeTask = promiseTask;
            }
        }
        if (removeTask != null) {
            boolean remove = false;
            for (AbstractPromiseTask promiseTask : array) {
                if (!remove) {
                    abstractPromise.getQueueTask().getMainQueue().add(promiseTask);
                }
                if (promiseTask.equals(removeTask)) {
                    remove = true;
                }
            }
        }
        return this;
    }

    public PromiseTest removeBefore(String index) {
        AbstractPromiseTask[] array = abstractPromise.getQueueTask().getMainQueue().toArray(new AbstractPromiseTask[0]);
        abstractPromise.getQueueTask().getMainQueue().clear();
        index = abstractPromise.getComplexIndex(index);
        boolean remove = true;
        for (AbstractPromiseTask promiseTask : array) {
            if (promiseTask.getNamespace().equals(index)) {
                remove = false;
            }
            if (!remove) {
                abstractPromise.getQueueTask().getMainQueue().add(promiseTask);
            }
        }
        return this;
    }

    public List<String> getIndex() {
        List<String> result = new ArrayList<>();
        AbstractPromiseTask[] array = abstractPromise.getQueueTask().getMainQueue().toArray(new AbstractPromiseTask[0]);
        for (AbstractPromiseTask promiseTask : array) {
            result.add(
                    promiseTask.getNamespace().substring(abstractPromise.getIndex().length() + 1)
                            + "::" + promiseTask.getType().toString()
            );
        }
        return result;
    }


    public AbstractPromiseTask get(String index, PromiseTaskExecuteType type) {
        AbstractPromiseTask[] array = abstractPromise.getQueueTask().getMainQueue().toArray(new AbstractPromiseTask[0]);
        abstractPromise.getQueueTask().getMainQueue().clear();
        index = abstractPromise.getComplexIndex(index);
        for (AbstractPromiseTask promiseTask : array) {
            if (promiseTask.getNamespace().equals(index) && promiseTask.getType().equals(type)) {
                return promiseTask;
            }
        }
        return null;
    }

}
