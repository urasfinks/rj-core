package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.batch.writer.AbstractAsyncFileWriterElement;
import ru.jamsys.core.extension.batch.writer.AsyncFileWriterElement;
import ru.jamsys.core.extension.batch.writer.AsyncFileWriterRolling;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BrokerPersistTest {

    @Getter
    @Setter
    public static class X implements ByteSerialization {

        private String value;

        public X(String value) {
            this.value = value;
        }

        @Override
        public byte[] toByte() {
            return value.getBytes();
        }

        @Override
        public void toObject(byte[] bytes) {
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
    public void test() throws Throwable {
        ConcurrentLinkedDeque<AbstractAsyncFileWriterElement> outputQueue = new ConcurrentLinkedDeque<>();

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
        BrokerPersist<X> test = App.get(Manager.class).configure(
                BrokerPersist.class,
                "test",
                s -> new BrokerPersist<>(s, App.context, (s1, dataPayload) -> null)
        ).getGeneric();
        test.add(new X("Hello"));
        // Данные добавлены в очередь на запись, но реально ещё не сохранились на файловую систему. То есть в последний
        // коммит контроллер они упадут только после записи и на текущий момент размер = 0
        Assertions.assertEquals(0, test.getLastCommitConfiguration().get().getBinFileReaderResult().getSize().get());
        // Записали на фс данные
        test.getBinConfiguration().get().flush(run);
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
                Files.readAllBytes(Paths.get(test.getBinConfiguration().get().getFilePath()))
        );

        // В последнем коммит контроллере она должны появиться
        Assertions.assertEquals(1, test.getLastCommitConfiguration().get().getBinFileReaderResult().size());
        // Забираем элемент на обработку
        BrokerPersistElement<X> poll = test.poll();
        Assertions.assertEquals("Hello", new String(poll.getBytes()));
        // Теперь надо закоммитить
        test.commit(poll);
        // Должны получить, что элементов пока ещё 1, так как не произошла запись на диск
        Assertions.assertEquals(1, test.getLastCommitConfiguration().get().getBinFileReaderResult().size());
        // Запускаем запись wal
        test
                .getLastCommitConfiguration().get()
                .getCommitControllerConfiguration().get()
                .flush(run);
        // Теперь после записи не должно остаться не обработанных элементов
        Assertions.assertEquals(0, test.getLastCommitConfiguration().get().getBinFileReaderResult().size());

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
                Files.readAllBytes(Paths.get(test.getLastCommitConfiguration().get().getFilePathCommit()))
        );
        Assertions.assertFalse(test.getLastCommitConfiguration().get().isComplete());
        // Осталось обработать позиций 0
        Assertions.assertEquals(0, test.getLastCommitConfiguration().get().getBinFileReaderResult().size());
        // Статус оригинального файла - не завершён
        Assertions.assertFalse(test.getLastCommitConfiguration().get().getBinFileReaderResult().isFinishState());

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
                Files.readAllBytes(Paths.get(test.getBinConfiguration().get().getFilePath()))
        );
        // Проверим, что оригинальный файл достиг конца
        Assertions.assertTrue(test.getLastCommitConfiguration().get().getBinFileReaderResult().isFinishState());
        // Проверим что файл удалился так как завершился CommitController и он был isComplete
        Assertions.assertFalse(Files.exists(Paths.get(test.getLastCommitConfiguration().get().getFilePathCommit())));

    }
}