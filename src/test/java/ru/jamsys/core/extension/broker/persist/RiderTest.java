package ru.jamsys.core.extension.broker.persist;

import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerConfiguration;
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
        App.get(ServiceProperty.class).set("$.BrokerPersist.test.directory", "1LogPersist");
    }

    @BeforeEach
    void beforeEach() {
        UtilFile.removeAllFilesInFolder("1LogPersist");
    }

    @AfterAll
    static void shutdown() {
        UtilFile.removeAllFilesInFolder("1LogPersist");
        App.shutdown();
    }

    @SuppressWarnings("all")
    @Test
    public void test() throws IOException {
        BrokerPersistRepositoryProperty brokerPersistRepositoryProperty = new BrokerPersistRepositoryProperty();
        PropertyDispatcher<Object> test = new PropertyDispatcher<>(
                null,
                brokerPersistRepositoryProperty,
                "$.BrokerPersist.test"
        );
        test.run();

        UtilFile.writeBytes("1LogPersist/test.bin", ((Supplier<byte[]>) () -> {
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

        UtilFile.writeBytes("1LogPersist/test.bin.commit", ((Supplier<byte[]>) () -> {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                output.write(UtilByte.intToBytes(8));
                output.write(UtilByte.longToBytes(0));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return output.toByteArray();
        }).get(), FileWriteOptions.CREATE_OR_REPLACE);

        ManagerConfiguration<Rider> riderManagerConfiguration = ManagerConfiguration.getInstance(
                Rider.class,
                java.util.UUID.randomUUID().toString(),
                "test",
                managerElement -> {
                    managerElement.setup(
                            "1LogPersist/test.bin",
                            brokerPersistRepositoryProperty,
                            null,
                            true
                    );
                }
        );

        Rider rider = riderManagerConfiguration.get();
        Assertions.assertEquals(1, rider.getQueueRetry().size());
        Assertions.assertEquals("world", new String(rider.getQueueRetry().getForUnitTest(9L).getBytes()));
    }
}