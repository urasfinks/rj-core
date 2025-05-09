package ru.jamsys.core.component;

import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.promise.AbstractPromiseTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Задача в виртуальном потоке может вызваться непонятно когда и так же исполнится непонятно когда
// В task.run() должно контролироваться, что Promise как бы ещё готов исполнятся, а то зачем всё это
// Контроль исполнения по tps тут сомнителен + сервис глобальный (нет разделения по задачам)

@Component
public class ServiceThreadVirtual extends AbstractLifeCycle implements LifeCycleComponent {

    private final ExecutorService executorService = Executors
            .newThreadPerTaskExecutor(Thread.ofVirtual().name("v-thread-", 0).factory());

    public void execute(AbstractPromiseTask arguments) {
        arguments.setThreadRun(getRun());
        executorService.submit(arguments);
    }

    @Override
    public int getInitializationIndex() {
        return 1;
    }

    @Override
    public void runOperation() {

    }

    @Override
    public void shutdownOperation() {
        executorService.shutdown();
    }

}
