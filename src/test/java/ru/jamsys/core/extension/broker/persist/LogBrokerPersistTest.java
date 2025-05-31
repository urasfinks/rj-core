package ru.jamsys.core.extension.broker.persist;

import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.log.Log;
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
        ManagerConfiguration<BrokerPersist<Log>> brokerPersistManagerConfiguration = ManagerConfiguration.getInstance(
                BrokerPersist.class,
                java.util.UUID.randomUUID().toString(),
                "test",
                managerElement -> managerElement.setupRestoreElementFromByte((bytes) -> {
                    try {
                        return (Log) Log.instanceFromBytes(bytes);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
        );
        BrokerPersist<Log> test = brokerPersistManagerConfiguration.get();

        int c = 1_000_000;
        AtomicInteger write = new AtomicInteger(0);
        long s1 = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            Thread thread = new Thread(() -> {
                int c1 = c / 4;
                for (int j = 0; j < c1; j++) {
                    test.add(UtilLog.info("Hello world"));
                    write.incrementAndGet();
                }
            });
            thread.start();
            thread.join();
        }

        System.out.println("add: " + (System.currentTimeMillis() - s1));

        for (int i = 0; i < 4; i++) {
            Thread thread = new Thread(() -> {
                while (write.get() > 0) {
                    X<Log> poll = test.poll();
                    if (poll != null) {
                        test.commit(poll);
                        write.decrementAndGet();
                    }
                }
            });
            thread.start();
        }
        Util.await(
                5000,
                100,
                () -> write.get() == 0,
                timing -> UtilLog.printInfo("Success all commit; timing:" + timing),
                () -> Assertions.fail("Error all commit: " + write.get())
        );

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
                () -> Assertions.fail("Error: " + test.getLastRiderConfiguration().get().getQueueRetry().size())
        );
        Assertions.assertEquals(1, test.getMapRiderConfiguration().size());
    }


}