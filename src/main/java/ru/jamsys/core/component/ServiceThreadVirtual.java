package ru.jamsys.core.component;

import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.promise.PromiseTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wow it's amazing!
 *
 * <p>
 * <pre> {@code
 * int x = x + 1;
 * }</pre>
 */

// Задача в виртуальном потоке может вызваться непонятно когда и так же исполнится непонятно когда
// В task.run() должно контролироваться, что Promise как бы ещё готов исполнятся, а то зачем всё это
// Контроль исполнения по tps тут сомнителен + сервис глобальный (нет разделения по задачам)

@Component
public class ServiceThreadVirtual implements LifeCycleComponent {

    private final ExecutorService executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("v-thread-", 0).factory());

    private final AtomicBoolean run = new AtomicBoolean(true);

    public void execute(PromiseTask arguments) {
        arguments.setThreadRun(run);
        executorService.submit(arguments);
    }

    @Override
    public int getInitializationIndex() {
        return 1;
    }

    @Override
    public boolean isRun() {
        return run.get();
    }

    @Override
    public void run() {
        run.set(true);
    }

    @Override
    public void shutdown() {
        run.set(false);
        executorService.shutdown();
    }

}
