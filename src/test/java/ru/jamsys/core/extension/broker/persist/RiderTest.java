package ru.jamsys.core.extension.broker.persist;

import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
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

        Rider rider = App.get(Manager.class).configure(
                Rider.class,
                "test",
                key1 -> new Rider(
                        App.context,
                        key1,
                        "LogManager/test.bin.commit",
                        rider1 -> {
                        }
                )
        ).get();
        Assertions.assertEquals(1, rider.getQueueRetry().size());
        Assertions.assertEquals("world", new String(rider.getQueueRetry().getUnique().get(9L).getBytes()));
    }
}