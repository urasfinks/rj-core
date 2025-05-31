package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.ByteCodec;
import ru.jamsys.core.extension.async.writer.Position;
import ru.jamsys.core.extension.async.writer.QueueRetry;
import ru.jamsys.core.extension.expiration.ExpirationList;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BrokerPersistTest {

    @Getter
    @Setter
    public static class TestElement implements ByteCodec, Position {

        private long position;

        private String value;

        public TestElement(String value) {
            this.value = value;
        }

        @Override
        public byte[] toBytes() {
            return value.getBytes();
        }

        @Override
        public void fromBytes(byte[] bytes) {
            setValue(new String(bytes));
        }

    }

    AtomicBoolean run = new AtomicBoolean(true);

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runSpring();
    }

    @BeforeEach
    void beforeEach() {
        UtilFile.removeAllFilesInFolder("LogManager");
        if (App.get(Manager.class).contains(ExpirationList.class, QueueRetry.class.getName(), QueueRetry.class.getName())) {
            App.get(Manager.class).get(ExpirationList.class, QueueRetry.class.getName(), QueueRetry.class.getName(), null).unitTestReset();
        }
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @SuppressWarnings("all")
    @Test
    public void test1() throws Throwable {
        App.get(ServiceProperty.class).set("App.BrokerPersist.test1.directory", "LogManager");
        ManagerConfiguration<BrokerPersist<TestElement>> brokerPersistManagerConfiguration = ManagerConfiguration.getInstance(
                BrokerPersist.class,
                java.util.UUID.randomUUID().toString(),
                "test1",
                managerElement -> {
                }
        );
        BrokerPersist<TestElement> test = brokerPersistManagerConfiguration.get();

        test.add(new TestElement("Hello"));
        // Данные добавлены в очередь на запись, но реально ещё не сохранились на файловую систему. То есть в последний
        // rider они упадут только после записи и на текущий момент размер = 0
        Assertions.assertEquals(0, test.getLastRiderConfiguration().get().getQueueRetry().size());
        // Записали на фс данные
        test.getXWriterConfiguration().get().flush(run);
        assertArrayEquals(
                ((Supplier<byte[]>) () -> {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    try {
                        output.write(UtilByte.intToBytes(5));
                        output.write("Hello".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return output.toByteArray();
                }).get(),
                Files.readAllBytes(Paths.get(test.getXWriterConfiguration().get().getFilePath()))
        );

        // Должна появится 1 не обработанная запись
        Assertions.assertEquals(1, test.getLastRiderConfiguration().get().getQueueRetry().size());
        // Забираем элемент на обработку
        X<TestElement> poll = test.poll();
        Assertions.assertEquals("Hello", new String(poll.toBytes()));
        // Теперь надо закоммитить
        test.commit(poll);
        // Должны получить, что элементов пока ещё 1, так как не произошла запись на диск
        Assertions.assertEquals(1, test.getLastRiderConfiguration().get().getQueueRetry().size());
        // Запускаем запись wal
        test
                .getLastRiderConfiguration().get()
                .getYWriterConfiguration().get()
                .flush(run);
        // Теперь после записи не должно остаться не обработанных элементов
        Assertions.assertEquals(0, test.getLastRiderConfiguration().get().getQueueRetry().size());

        assertArrayEquals(
                ((Supplier<byte[]>) () -> {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    try {
                        output.write(UtilByte.intToBytes(8));
                        output.write(UtilByte.longToBytes(0));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return output.toByteArray();
                }).get(),
                Files.readAllBytes(Paths.get(test.getLastRiderConfiguration().get().getFilePathY()))
        );
        Assertions.assertFalse(test.getLastRiderConfiguration().get().getQueueRetry().isProcessed());
        // Осталось обработать позиций 0
        Assertions.assertEquals(0, test.getLastRiderConfiguration().get().getQueueRetry().size());
        // Статус оригинального файла - не завершён
        Assertions.assertFalse(test.getLastRiderConfiguration().get().getQueueRetry().isFinishState());

        test.getXWriterConfiguration().get().shutdown();

        // Проверим что записался в конце -1
        assertArrayEquals(
                ((Supplier<byte[]>) () -> {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    try {
                        output.write(UtilByte.intToBytes(5));
                        output.write("Hello".getBytes());
                        output.write(UtilByte.intToBytes(-1));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return output.toByteArray();
                }).get(),
                Files.readAllBytes(Paths.get(test.getXWriterConfiguration().get().getFilePath()))
        );
        test.getLastRiderConfiguration().get().shutdown();
        // Проверим что файл удалился так как завершился CommitController и он был isComplete
        Assertions.assertFalse(Files.exists(Paths.get(test.getLastRiderConfiguration().get().getFilePathY())));

    }

    @SuppressWarnings("all")
    @Test
    void test2() throws Throwable {
        UtilFile.removeAllFilesInFolder("LogManager");
        App.get(ServiceProperty.class).set("App.BrokerPersist.test2.directory", "LogManager");
        App.get(ServiceProperty.class).set("App.AsyncFileWriterWal[App.BrokerPersist.test2::LogManager/test2.afwr].flush.max.time.ms", "99999999");

        // 2 записи; 0 коммитов
        UtilFile.writeBytes("LogManager/test1.afwr", ((Supplier<byte[]>) () -> {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                output.write(UtilByte.intToBytes(5));
                output.write("Hello".getBytes());
                output.write(UtilByte.intToBytes(5));
                output.write("world".getBytes());
                output.write(UtilByte.intToBytes(-1));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return output.toByteArray();
        }).get(), FileWriteOptions.CREATE_OR_REPLACE);

        UtilFile.createNewFile("LogManager/test1.afwr.commit");

        // 2 записи; 1 коммит
        UtilFile.writeBytes("LogManager/test2.afwr", ((Supplier<byte[]>) () -> {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                output.write(UtilByte.intToBytes(5));
                output.write("opa__".getBytes());
                output.write(UtilByte.intToBytes(5));
                output.write("cha__".getBytes());
                output.write(UtilByte.intToBytes(-1));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return output.toByteArray();
        }).get(), FileWriteOptions.CREATE_OR_REPLACE);

        UtilFile.writeBytes("LogManager/test2.afwr.commit", ((Supplier<byte[]>) () -> {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                output.write(UtilByte.intToBytes(8));
                output.write(UtilByte.longToBytes(0));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return output.toByteArray();
        }).get(), FileWriteOptions.CREATE_OR_REPLACE);

        ManagerConfiguration<BrokerPersist<TestElement>> brokerPersistManagerConfiguration = ManagerConfiguration.getInstance(
                BrokerPersist.class,
                java.util.UUID.randomUUID().toString(),
                "test2",
                managerElement -> managerElement.setupRestoreElementFromByte((bytes) -> new TestElement(new String(bytes)))
        );

        BrokerPersist<TestElement> test = brokerPersistManagerConfiguration.get();

        Assertions.assertEquals(2, test.getMapRiderConfiguration().size());
        X<TestElement> poll = test.poll();
        // Очередь последнего rider полностью вычитана
        Assertions.assertEquals("LogManager/test2.afwr.commit", test.getLastRiderConfiguration().get().getFilePathY());
        Assertions.assertEquals(1, test.getLastRiderConfiguration().get().getQueueRetry().size());

        // В данный момент последний райдер не завершён, так как ждёт коммита выданного элемента
        Assertions.assertEquals("cha__", poll.getElement().getValue());
        Assertions.assertFalse(test.isEmpty());

        // Изъятый элемент должен светиться в последнем rider в expirationList
        Assertions.assertEquals(1, test
                .getLastRiderConfiguration()
                .get()
                .getQueueRetry()
                .getExpirationListConfiguration()
                .get()
                .size()
        );

        Rider lastRider = test.getLastRiderConfiguration().get();

        // Теперь закоммитим изъятый элемент и должен будет завершиться 1 райдер, должны будем взять второй и наполнить
        // + 2 элемента
        test.commit(poll);
        Assertions.assertEquals(1, test
                .getLastRiderConfiguration()
                .get()
                .getYWriterConfiguration()
                .get()
                .getInputQueue()
                .size()
        );
        test
                .getLastRiderConfiguration()
                .get()
                .getYWriterConfiguration()
                .get()
                .flush(run);

        Assertions.assertEquals(0, test
                .getLastRiderConfiguration()
                .get()
                .getYWriterConfiguration()
                .get()
                .getInputQueue()
                .size()
        );

        // Проверяем что rider преисполнился, то есть у него в очередях ничего нет и нет в expirationList ожиданий
        // и файл по которому мы читали записан до конца (finishState)
        Assertions.assertEquals("LogManager/test2.afwr.commit", lastRider.getFilePathY());
        Assertions.assertTrue(lastRider.getQueueRetry().isProcessed());

        // Эмулируем работу менеджера, завершаем rider
        lastRider.shutdown();

        // Проверяем что файла больше нет
        Assertions.assertFalse(UtilFile.ifExist("LogManager/test2.afwr.commit"));
        // После удаления ещё должен поменяться и
        Assertions.assertEquals("LogManager/test1.afwr.commit", test.getLastRiderConfiguration().get().getFilePathY());

        // После удаления rider в мапе должен остаться только 1
        Assertions.assertEquals(1, test.getMapRiderConfiguration().size());

        // проверим, что rider остался ещё 1 элемент
        Assertions.assertEquals(2, test.getLastRiderConfiguration().get().getQueueRetry().size());

        X<TestElement> poll1 = test.poll();
        Assertions.assertEquals(2, test.getLastRiderConfiguration().get().getQueueRetry().size());
        X<TestElement> poll2 = test.poll();

        Assertions.assertThrows(RuntimeException.class, () -> test.commit(poll));

        test.commit(poll1);
        test.commit(poll2);

        // Но при этом размер rider ещё 2, так как он ещё на файловую систему не сброшено состояние
        Assertions.assertEquals(2, test.getLastRiderConfiguration().get().getQueueRetry().size());

        test
                .getLastRiderConfiguration()
                .get()
                .getYWriterConfiguration()
                .get()
                .flush(run);

        Assertions.assertNull(test.getLastRiderConfiguration());


    }

}