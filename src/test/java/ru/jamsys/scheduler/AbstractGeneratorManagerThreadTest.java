package ru.jamsys.scheduler;

import org.junit.jupiter.api.Test;

import ru.jamsys.task.Task;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

class AbstractGeneratorManagerThreadTest {

    @Test
    public void test() {
        //TODO:
//        ExecutorServiceScheduler service = new ExecutorServiceScheduler(1000);
//        Task task = new Task((AtomicBoolean isWhile) -> {
//            long time = System.currentTimeMillis();
//            int count = 0;
//            for (long i = 0; i < Long.MAX_VALUE; i++) {
//                count++;
//            }
//            if (isWhile.get()) {
//                Util.logConsole("count: " + count + "; time: " + (System.currentTimeMillis() - time));
//            }
//        }, 2000);
//        task.getTags().put("name", "word");
//
//        service.getListTaskHandler().add(task);
//        service.run();
//
//        Util.sleepMillis(3000);
//        service.getTimeManager().flush();
//
//        Util.sleepMillis(7000);
//        service.getTimeManager().flush();
//        Util.sleepMillis(7000);
//        service.shutdown();
    }

}