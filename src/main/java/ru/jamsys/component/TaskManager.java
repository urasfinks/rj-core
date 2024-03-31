package ru.jamsys.component;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.pool.AutoBalancerPool;
import ru.jamsys.pool.ThreadPool;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.handler.Handler;
import ru.jamsys.thread.task.Task;

@Component
public class TaskManager extends AutoBalancerPool<ThreadPool> implements KeepAliveComponent, StatisticsCollectorComponent {

    final private Broker broker;

    final private ExceptionHandler exceptionHandler;

    final private Dictionary dictionary;

    public TaskManager(Broker broker, ExceptionHandler exceptionHandler, Dictionary dictionary, ApplicationContext applicationContext) {
        super(applicationContext);
        this.broker = broker;
        this.exceptionHandler = exceptionHandler;
        this.dictionary = dictionary;
    }

    public void addTask(Task task) throws Exception {
        String taskIndex = task.getIndex();
        if (!mapPool.containsKey(taskIndex)) {
            ThreadPool threadPool = createThreadPool(taskIndex);
            mapPool.putIfAbsent(taskIndex, threadPool);
            threadPool.run();
        }
        broker.add(taskIndex, (BrokerCollectible) task);
        mapPool.get(taskIndex).wakeUp();
    }

    public void executeTask(ThreadEnvelope threadEnvelope, Task task) {
        @SuppressWarnings("unchecked")
        Handler<Task> handler = dictionary.getTaskHandler().get(task.getClass());
        if (handler != null) {
            TaskStatistic taskStatistic = getTaskStatistic(threadEnvelope, task);
            try {
                handler.run(task, threadEnvelope);
            } catch (Exception e) {
                exceptionHandler.handler(e);
            }
            taskStatistic.finish();
        } else {
            exceptionHandler.handler(new RuntimeException("Not find TaskHandler for Task = " + task.getClass()));
        }
    }

    private ThreadPool createThreadPool(String poolName) {
        return new ThreadPool(
                poolName,
                0,
                1,
                (ThreadEnvelope threadEnvelope) -> {
                    Task task = broker.pollLast(poolName);
                    if (task == null) {
                        return false;
                    }
                    executeTask(threadEnvelope, task);
                    return true;
                }
        );
    }

}
