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
        UtilFile.removeAllFilesInFolder("1LogPersist");
    }

    @Test
    void linearTest() throws InterruptedException {
        App.get(ServiceProperty.class).set("$.BrokerPersist.test.directory", "1LogPersist");
        ManagerConfiguration<BrokerPersist<Log>> brokerPersistManagerConfiguration = ManagerConfiguration.getInstance(
                "test",
                java.util.UUID.randomUUID().toString(),
                BrokerPersist.class,
                managerElement -> managerElement.setup((bytes) -> {
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
                    try {
                        test.add(UtilLog.info("Hello world"));
                        write.incrementAndGet();
                    } catch (Exception e) {
                        App.error(e);
                    }
                }
            });
            thread.start();
            thread.join();
        }
        UtilLog.printInfo("add: " + (System.currentTimeMillis() - s1));

        for (int i = 0; i < 4; i++) {
            Thread thread = new Thread(() -> {
                while (write.get() > 0) {
                    BlockData<Log> poll = test.poll();
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
                        if (config.get().getQueueRetry().sizeWait() > 0) {
                            return false;
                        }
                    }
                    return true;
                },
                (timing) -> UtilLog.printInfo("Success: " + timing),
                () -> Assertions.fail("Error: " + test.getLastRiderConfiguration().get().getQueueRetry().sizeWait())
        );
        Assertions.assertEquals(1, test.getMapRiderConfiguration().size());
    }


}