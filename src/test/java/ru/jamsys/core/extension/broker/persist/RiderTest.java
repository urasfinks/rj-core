package ru.jamsys.core.extension.broker.persist;

import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.broker.BrokerPersistRepositoryProperty;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

class RiderTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runSpring();
        App.get(ServiceProperty.class).set("App.BrokerPersist.test.directory", "LogManager");
    }

    @BeforeEach
    void beforeEach() {
        UtilFile.removeAllFilesInFolder("LogManager");
    }

    @AfterAll
    static void shutdown() {
        UtilFile.removeAllFilesInFolder("LogManager");
        App.shutdown();
    }

    @Test
    public void test() throws IOException {
        BrokerPersistRepositoryProperty brokerPersistRepositoryProperty = new BrokerPersistRepositoryProperty();
        PropertyDispatcher<Object> test = new PropertyDispatcher<>(
                App.get(ServiceProperty.class, App.context),
                null,
                brokerPersistRepositoryProperty,
                "App.BrokerPersist.test"
        );
        test.run();

        UtilFile.writeBytes("LogManager/test.bin", ((Supplier<byte[]>) () -> {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                output.write(UtilByte.intToBytes(5));
                output.write("Hello".getBytes());
                output.write(UtilByte.intToBytes(5));
                output.write("world".getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return output.toByteArray();
        }).get(), FileWriteOptions.CREATE_OR_REPLACE);

        UtilFile.writeBytes("LogManager/test.bin.commit", ((Supplier<byte[]>) () -> {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                output.write(UtilByte.intToBytes(8));
                output.write(UtilByte.longToBytes(0));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return output.toByteArray();
        }).get(), FileWriteOptions.CREATE_OR_REPLACE);

        Rider rider = App.get(Manager.class).getManagerConfiguration(
                Rider.class,
                "test",
                key1 -> new Rider(
                        brokerPersistRepositoryProperty,
                        key1,
                        "LogManager/test.bin",
                        true,
                        _ -> {
                        }
                )
        ).get();
        Assertions.assertEquals(1, rider.getQueueRetry().size());
        Assertions.assertEquals("world", new String(rider.getQueueRetry().getForUnitTest(9L).getBytes()));
    }
}