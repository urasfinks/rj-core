package ru.jamsys.scheduler;

import org.junit.jupiter.api.Test;
import ru.jamsys.Util;
import ru.jamsys.task.Task;

import java.util.concurrent.atomic.AtomicBoolean;

class AbstractSchedulerThreadTest {

    @Test
    public void test() throws InterruptedException {
        NewThread newThread = new NewThread(1000);
        Task task = new Task((AtomicBoolean isRun) -> {
            Util.sleepMillis(10000);
            Util.logConsole("world");
        }, 2000);
        task.getTags().put("name", "word");
        newThread.getListTask().add(task);
        newThread.run();


        Util.sleepMillis(3000);
        newThread.getTimeManager().flush();
        Util.sleepMillis(5000);
        newThread.getTimeManager().flush();
        newThread.shutdown();
    }

}