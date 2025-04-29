package ru.jamsys.core.extension.broker.persist;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class WalFileWriterTest {


    private Path tempFile;
    private WalFileWriter walWriter;
    private long testCapacity;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("wal_test", ".dat");
        testCapacity = WalFileWriter.RECORD_SIZE * 3; // Очень маленькая capacity для тестирования расширения
        walWriter = new WalFileWriter(tempFile.toString(), testCapacity);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (walWriter != null) {
            walWriter.close();
        }
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testInitialFileSize() throws IOException {
        assertEquals(testCapacity, Files.size(tempFile));
    }

    @Test
    void testWriteWithinCapacity() throws IOException {
        walWriter.write(1L, (short) 1);
        assertEquals(testCapacity, Files.size(tempFile)); // Размер не должен измениться
        assertEquals(WalFileWriter.RECORD_SIZE, walWriter.dataOffset.get());
    }

    @Test
    void testFileExtensionWhenExceedingCapacity() throws IOException {
        assertEquals(WalFileWriter.RECORD_SIZE * 3, Files.size(tempFile));
        // Записываем ровно столько, сколько вмещается в начальный размер
        walWriter.write(1L, (short) 1);
        walWriter.write(2L, (short) 2);
        walWriter.write(3L, (short) 3);

        assertEquals(WalFileWriter.RECORD_SIZE * 3, Files.size(tempFile));
        // Следующая запись должна вызвать расширение
        walWriter.write(4L, (short) 4);

        assertEquals(WalFileWriter.RECORD_SIZE * 4, Files.size(tempFile));
        assertEquals(4 * WalFileWriter.RECORD_SIZE, walWriter.dataOffset.get());
    }

    @Test
    void testWriteLocking() throws IOException {
        // Первая запись должна заблокировать диапазон
        walWriter.write(1L, (short) 1);

        // Попытка записать в тот же диапазон из другого потока
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                walWriter.write(1L, (short) 1); // Та же позиция
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Должен завершиться без ошибок, так как блокировки на уровне записи
        assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS));
        executor.shutdown();
    }

    @Test
    void testCloseReleasesResources() throws IOException {
        walWriter.write(1L, (short) 1);
        walWriter.close();

        assertThrows(IOException.class, () -> walWriter.write(2L, (short) 2));
    }

    @Test
    void testTiming() throws IOException {
        walWriter.setCapacity(400_000);
        long time = System.currentTimeMillis();
        for (int i = 0; i < 400_000; i++) {
            walWriter.write(1L, (short) 1);
        }
        UtilLog.printInfo(WalFileWriterTest.class, "timing: " + (System.currentTimeMillis() - time));
    }

}