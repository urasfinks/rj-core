package ru.jamsys.core.extension.broker.persist;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.batch.writer.AbstractAsyncFileWriterElement;
import ru.jamsys.core.extension.batch.writer.AsyncFileWriterElement;
import ru.jamsys.core.extension.batch.writer.AsyncFileWriterRolling;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilLog;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

class CommitControllerTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runSpring();
        App.get(ServiceProperty.class).set("App.BrokerPersist.test.directory", "LogManager");
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    public void test() throws Throwable {
        ConcurrentLinkedDeque<AbstractAsyncFileWriterElement> outputQueue = new ConcurrentLinkedDeque<>();
        AtomicBoolean run = new AtomicBoolean(true);
        AsyncFileWriterRolling<AbstractAsyncFileWriterElement> writer = new AsyncFileWriterRolling<>(
                App.context,
                "test",
                "LogManager",
                outputQueue::addAll,
                fileName -> System.out.println("SWAP: " + fileName)
        );
        writer.run();
        writer.writeAsync(new AsyncFileWriterElement("Hello".getBytes()));
        writer.writeAsync(new AsyncFileWriterElement("world".getBytes()));
        writer.flush(run);
        Util.testSleepMs(100);
        UtilLog.printInfo(outputQueue);

        writer.shutdown();
    }

    @Test
    public void test2() throws Throwable {
        Manager.Configuration<BrokerPersist> test = App.get(Manager.class).configure(
                BrokerPersist.class,
                "test",
                s -> new BrokerPersist<>(s, App.context)
        );
        test.get().add(new BrokerPersistTest.X("Hello"));
    }
}