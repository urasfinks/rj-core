package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.ByteCodec;
import ru.jamsys.core.extension.batch.writer.Position;
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

        App.get(ServiceProperty.class).set("App.BrokerPersist.test.directory", "LogManager");
    }

    @BeforeEach
    void beforeEach() {
        UtilFile.removeAllFilesInFolder("LogManager");
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    public void test2() throws Throwable {
        BrokerPersist<TestElement> test = App.get(Manager.class).configure(
                BrokerPersist.class,
                "test",
                s -> new BrokerPersist<>(s, App.context, (_) -> null)
        ).getGeneric();
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

        // В последнем коммит контроллере она должны появиться
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
        Assertions.assertFalse(test.getLastRiderConfiguration().get().isComplete());
        // Осталось обработать позиций 0
        Assertions.assertEquals(0, test.getLastRiderConfiguration().get().getQueueRetry().size());
        // Статус оригинального файла - не завершён
        Assertions.assertFalse(test.getLastRiderConfiguration().get().getQueueRetry().isFinishState());

        test.shutdown();

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
        // Проверим, что оригинальный файл достиг конца
        Assertions.assertTrue(test.getLastRiderConfiguration().get().getQueueRetry().isFinishState());
        // Проверим что файл удалился так как завершился CommitController и он был isComplete
        Assertions.assertFalse(Files.exists(Paths.get(test.getLastRiderConfiguration().get().getFilePathY())));

    }

    @Test
    void test3() throws Throwable {
        UtilFile.removeAllFilesInFolder("LogManager");
        App.get(ServiceProperty.class).set("App.BrokerPersist.test.fill.threshold.min", "1");
        App.get(ServiceProperty.class).set("App.BrokerPersist.test.fill.threshold.max", "1");
        App.get(ServiceProperty.class).set("App.AsyncFileWriterWal[App.BrokerPersist.test::LogManager/test2.afwr].flush.max.time.ms", "99999999");


        UtilFile.writeBytes("LogManager/test1.afwr", ((Supplier<byte[]>) () -> {
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

        UtilFile.createNewFile("LogManager/test1.afwr.commit");

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

        BrokerPersist<TestElement> test = App.get(Manager.class).configure(
                BrokerPersist.class,
                "test",
                s -> new BrokerPersist<>(s, App.context, (bytes) -> new TestElement(new String(bytes)))
        ).getGeneric();

        Assertions.assertEquals(2, test.getMapRiderConfiguration().size());
        // Размер очереди с данными 0 так как ещё не вызывался помошник, который загрузит с файловой системы данные
        Assertions.assertEquals(0, test.size());
        //UtilLog.printInfo(test.getPropertyBroker());
        // запускаем помошника, он должен взять последнего rider и восстановить данные с диска + opa__  закоммичен,
        // а cha__ должен вернуться
        test.helper();
        // Очередь последнего rider полностью вычитана
        Assertions.assertTrue(test.getLastRiderConfiguration().get().getQueueRetry().queueIsEmpty());
        Assertions.assertEquals(0, test.getLastRiderConfiguration().get().getQueueRetry().size());
        Assertions.assertEquals(1, test.size());
        // В данный момент последний райдер не завершён, так как ждёт коммита выданного элемента
        // Повторный helper не должен накидать более ничего в очередь
        test.helper();
        Assertions.assertEquals(1, test.size());
        X<TestElement> poll = test.poll();
        Assertions.assertEquals("cha__", poll.getElement().getValue());
        // так как изъяли, размер очереди должен стать 0
        Assertions.assertEquals(0, test.size());
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

        Assertions.assertTrue(test.getLastRiderConfiguration().get().isComplete());

        test.getLastRiderConfiguration().get().shutdown();


    }

}