package ru.jamsys.core.extension.broker.persist;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.cron.Helper1s;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.broker.persist.element.StatisticElement;
import ru.jamsys.core.flat.util.UtilFile;

import java.io.IOException;

class BrokerPersistStatTest {

    @BeforeAll
    static void beforeAll() {
//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//        long now = System.currentTimeMillis();
//        long initialDelay = 1000 - (now % 1000);
//        scheduler.scheduleAtFixedRate(
//                () -> {
//                    try {
//                        if (App.context != null) {
//                            App.get(Helper1s.class).generate().run();
//                        }
//                    } catch (Throwable th) {
//                        App.error(th);
//                    }
//                },
//                initialDelay,
//                1000,
//                TimeUnit.MILLISECONDS
//        );
    }

    @BeforeEach
    void beforeEach() throws IOException {
        UtilFile.removeAllFilesInFolder("1StatisticPersist");
        UtilFile.copyAllFilesRecursive("TestBrokerPersist", "1StatisticPersist");
        App.getRunBuilder().addTestArguments().runSpring();
    }

    @AfterAll
    static void shutdown() {
        UtilFile.removeAllFilesInFolder("1StatisticPersist");
        App.shutdown();
    }

    @Test
    public void test() throws Throwable {
        ManagerConfiguration<BrokerPersist<StatisticElement>> brokerPersistManagerConfiguration =
                ManagerConfiguration.getInstance(
                        "statistic",
                        java.util.UUID.randomUUID().toString(),
                        BrokerPersist.class,
                        managerElement -> managerElement.setup((bytes) -> new StatisticElement(new String(bytes)))
                );
        BrokerPersist<StatisticElement> statisticElementBrokerPersist = brokerPersistManagerConfiguration.get();
        while (true) {
            BlockData<StatisticElement> poll = statisticElementBrokerPersist.poll();
            if (poll == null) {
                break;
            }
            statisticElementBrokerPersist.commit(poll);
            App.get(Helper1s.class).generate().run().await(1000);
        }
        System.out.println(1);
    }

}