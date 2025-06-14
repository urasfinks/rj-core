package ru.jamsys.core.extension.async.writer;

import lombok.Getter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.ByteSerializable;
import ru.jamsys.core.extension.broker.persist.BrokerPersistRepositoryProperty;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.UtilFile;

import java.util.concurrent.atomic.AtomicBoolean;

class AsyncFileWriterWalTest {

    private final AtomicBoolean run = new AtomicBoolean(true);

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runSpring();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void test() throws Throwable {
        UtilFile.removeIfExist("1StatisticPersist/1.bin");
        BrokerPersistRepositoryProperty brokerPersistRepositoryProperty = new BrokerPersistRepositoryProperty();
        PropertyDispatcher<Object> test = new PropertyDispatcher<>(
                null,
                brokerPersistRepositoryProperty,
                "$.BrokerPersist.statistic"
        );
        ManagerConfiguration<AsyncFileWriterWal<TestElement>> yWriterConfiguration = ManagerConfiguration.getInstance(
                AsyncFileWriterWal.class,
                java.util.UUID.randomUUID().toString(),
                "1StatisticPersist/1.bin",
                managerElement -> {
                    System.out.println("NEW ELEMENT");
                    managerElement.setupRepositoryProperty(brokerPersistRepositoryProperty);
                    managerElement.setupOnWrite((_, listY) -> {
                    });
                }
        );
        yWriterConfiguration.get().writeAsync(new TestElement("Hello".getBytes()));
        yWriterConfiguration.get().writeAsync(new TestElement("Hello".getBytes()));
        yWriterConfiguration.get().flush(run);

        App.get(Manager.class).remove(
                yWriterConfiguration.getCls(),
                yWriterConfiguration.getKey(),
                yWriterConfiguration.getNs()
        );

        yWriterConfiguration.get().writeAsync(new TestElement("Hello".getBytes()));
        yWriterConfiguration.get().flush(run);

        SimpleDataReader yFileReaderResult = new SimpleDataReader();
        AbstractAsyncFileReader.read("1StatisticPersist/1.bin", yFileReaderResult);
        Assertions.assertEquals(3, yFileReaderResult.getQueue().size());
        UtilFile.removeIfExist("1StatisticPersist/1.bin");
    }

    // Реализация тестового элемента
    public static class TestElement implements Position, ByteSerializable {

        private final byte[] data;
        @Getter
        private long position = -1;

        public TestElement(byte[] data) {
            this.data = data;
        }

        @Override
        public byte[] toBytes() {
            return data;
        }

        @Override
        public void setPosition(long pos) {
            this.position = pos;
        }

    }
}