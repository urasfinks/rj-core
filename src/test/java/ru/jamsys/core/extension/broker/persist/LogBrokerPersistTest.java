package ru.jamsys.core.extension.broker.persist;

import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.component.manager.item.log.Log;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilLog;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

class LogBrokerPersistTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        UtilFile.removeAllFilesInFolder("LogManager");
    }

    @Test
    void linearTest() throws InterruptedException {
        App.get(ServiceProperty.class).set("App.BrokerPersist.test.directory", "LogManager");
        BrokerPersist<Log> test = App.get(Manager.class).getManagerConfiguration(
                BrokerPersist.class,
                "test",
                s -> new BrokerPersist<>(s, App.context, (bytes) -> {
                    try {
                        return Log.instanceFromBytes(bytes);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
        ).getGeneric();

        int c = 1_000_000;
        long s1 = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            Thread thread = new Thread(() -> {
                int c1 = c / 4;
                for (int j = 0; j < c1; j++) {
                    test.add(UtilLog.info("Hello world"));
                }
            });
            thread.start();
            thread.join();
        }

        System.out.println("add: " + (System.currentTimeMillis() - s1));
        AtomicInteger read = new AtomicInteger(0);
        long s2 = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            Thread thread = new Thread(() -> {
                while (read.get() < c) {
                    X<Log> poll = test.poll();
                    if (poll != null) {
                        test.commit(poll);
                        read.incrementAndGet();
                    }
                }
            });
            thread.start();
            thread.join();
        }

        Assertions.assertEquals(c, read.get());
        System.out.println("pool: " + (System.currentTimeMillis() - s2));
        //test.shutdown();
        Util.await(
                5_000,
                100,
                () -> {
                    for (Iterator<ManagerConfiguration<Rider>> it = test.getQueueRiderConfiguration().descendingIterator(); it.hasNext(); ) {
                        ManagerConfiguration<Rider> config = it.next();
                        if (config.get().getQueueRetry().size() > 0) {
                            return false;
                        }
                    }
                    return true;
                },
                (timing) -> UtilLog.printInfo("Success: " + timing),
                () -> UtilLog.printError("Error: " + test.getLastRiderConfiguration().get().getQueueRetry().size())
        );
//        if (test.getMapRiderConfiguration().size() == 2) {
//            System.out.println(1);
//        }
        Assertions.assertEquals(1, test.getMapRiderConfiguration().size());
    }


}