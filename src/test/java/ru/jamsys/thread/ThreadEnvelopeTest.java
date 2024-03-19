package ru.jamsys.thread;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

class ThreadEnvelopeTest {
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.context = SpringApplication.run(App.class, args);
    }

    @Test
    public void test() {
        ThreadPool threadPool = new ThreadPool(
                "TestPool",
                0,
                10,
                (AtomicBoolean isWhile, ThreadEnvelope threadEnvelope) -> {
                    while (true) {
                        Util.logConsole("Hey");
                        Util.sleepMs(2000);
                    }
                    //return false;
                }
        );
        threadPool.run();
        threadPool.testRun();

        for (int i = 0; i < 10; i++) {
            try {
                threadPool.wakeUp();
                Util.sleepMs(1000);
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }

        Util.sleepMs(25000);


//        ThreadEnvelope threadEnvelope = new ThreadEnvelope();
//        threadEnvelope.run();
//        Util.sleepMs(1000);
//        threadEnvelope.resume();
//        Util.sleepMs(1000);
//        threadEnvelope.resume();
//        Util.sleepMs(1000);
//        threadEnvelope.shutdown();
    }
}