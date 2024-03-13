package ru.jamsys.component;

import org.springframework.stereotype.Component;
import ru.jamsys.thread.task.Task;
import ru.jamsys.thread.handler.Handler;
import ru.jamsys.thread.ThreadPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TaskManager {

    final private Broker broker;

    final private ExceptionHandler exceptionHandler;

    final private Dictionary dictionary;

    final private Map<String, ThreadPool> mapPool = new ConcurrentHashMap<>();

    public TaskManager(Broker broker, ExceptionHandler exceptionHandler, Dictionary dictionary) {
        this.broker = broker;
        this.exceptionHandler = exceptionHandler;
        this.dictionary = dictionary;
    }

    public void add(Task task) {
        String index = task.getIndex();
        if (!mapPool.containsKey(index)) {
            addPool(index);
        }
        try {
            broker.get(index).add(task);
            mapPool.get(index).wakeUp();
        } catch (Exception e) {
            exceptionHandler.handler(e);
        }
    }

    private void addPool(String name) {
        if (!mapPool.containsKey(name)) {
            ThreadPool threadPool = new ThreadPool(name, 0, 1, 60000, (AtomicBoolean isWhile) -> {
                while (isWhile.get()) {
                    Task task = broker.pollLast(name);
                    if (task == null) {
                        return false;
                    }
                    @SuppressWarnings("unchecked")
                    Handler<Task> handler = dictionary.getTaskHandler().get(task.getClass());
                    if (handler != null) {
                        try {
                            handler.run(task, isWhile);
                        } catch (Exception e) {
                            exceptionHandler.handler(e);
                        }
                    } else {
                        exceptionHandler.handler(new RuntimeException("Not find TaskHandler for Task = " + task.getClass()));
                    }
                }
                return false;
            });
            mapPool.put(name, threadPool);
            threadPool.run();
        }
    }
}
