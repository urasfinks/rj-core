package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.jamsys.core.flat.util.UtilLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BatchFileWriterCallbackTest {

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Callback implements FileDataPosition {
        private long fileDataPosition;
        private int fileDataLength;
    }

    private static final int MIN_BATCH_SIZE = 4096;
    private Path testFile;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        testFile = tempDir.resolve("test.bin");
    }

    @Test
    void testWriterCreatesFile() throws Exception {

        assertFalse(Files.exists(testFile));

        try (BatchFileWriterCallback<Callback> _ = new BatchFileWriterCallback<>(testFile)) {
            assertTrue(Files.exists(testFile));
        }
    }

    @Test
    void testWriteSmallDataDoesNotFlushImmediately() throws Exception {

        byte[] smallData = new byte[100];
        Arrays.fill(smallData, (byte) 1);

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(smallData);
            assertEquals(0, Files.size(testFile));
        }

        // После закрытия данные должны быть записаны
        assertEquals(smallData.length, Files.size(testFile));
        assertArrayEquals(smallData, Files.readAllBytes(testFile));
    }

    @Test
    void testAutoFlushWhenBatchSizeReached() throws Exception {

        byte[] data1 = new byte[MIN_BATCH_SIZE - 100];
        byte[] data2 = new byte[200]; // Суммарно превысит MIN_BATCH_SIZE

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(data1);
            assertEquals(0, Files.size(testFile));

            writer.write(data2);
            // Должен произойти автоматический flush
            assertEquals(data1.length + data2.length, Files.size(testFile));
        }
    }

    @Test
    void testLargeDataWrittenImmediately() throws Exception {

        byte[] largeData = new byte[MIN_BATCH_SIZE * 2];
        Arrays.fill(largeData, (byte) 2);

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(largeData);
            // Большие данные должны записаться сразу
            assertEquals(largeData.length, Files.size(testFile));
        }
    }

    @Test
    void testMultipleWritesCorrectOrder() throws Exception {

        byte[] data1 = new byte[100];
        byte[] data2 = new byte[MIN_BATCH_SIZE];
        byte[] data3 = new byte[50];

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(data1);
            writer.write(data2);
            writer.write(data3);
        }

        byte[] allData = new byte[data1.length + data2.length + data3.length];
        System.arraycopy(data1, 0, allData, 0, data1.length);
        System.arraycopy(data2, 0, allData, data1.length, data2.length);
        System.arraycopy(data3, 0, allData, data1.length + data2.length, data3.length);

        assertArrayEquals(allData, Files.readAllBytes(testFile));
    }

    @Test
    void testBufferExpansionForLargeData() throws Exception {

        byte[] hugeData = new byte[MIN_BATCH_SIZE * 10];
        Arrays.fill(hugeData, (byte) 3);

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(hugeData);
            assertEquals(hugeData.length, Files.size(testFile));
        }
    }

    @Test
    void testCloseFlushesRemainingData() throws Exception {

        byte[] data = new byte[100];

        BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile);
        writer.write(data);
        assertEquals(0, Files.size(testFile));

        writer.close();
        assertEquals(data.length, Files.size(testFile));
    }

    @Test
    void testWriteAfterCloseThrowsException() throws Exception {

        BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile);
        writer.close();

        assertThrows(IOException.class, () -> writer.write(new byte[1]));
    }

    @Test
    void testFileContentCorrectness() throws Exception {

        // Генерируем тестовые данные с определенным паттерном
        byte[] pattern1 = new byte[100];
        for (int i = 0; i < pattern1.length; i++) {
            pattern1[i] = (byte) (i % 100);
        }

        byte[] pattern2 = new byte[MIN_BATCH_SIZE];
        for (int i = 0; i < pattern2.length; i++) {
            pattern2[i] = (byte) (i % 256);
        }

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(pattern1);
            writer.write(pattern2);
            writer.write(pattern1);
        }

        // Проверяем содержимое файла
        byte[] fileContent = Files.readAllBytes(testFile);
        assertEquals(pattern1.length + pattern2.length + pattern1.length, fileContent.length);

        // Проверяем первый блок
        assertArrayEquals(pattern1, Arrays.copyOfRange(fileContent, 0, pattern1.length));

        // Проверяем второй блок
        assertArrayEquals(pattern2, Arrays.copyOfRange(fileContent, pattern1.length, pattern1.length + pattern2.length));

        // Проверяем третий блок
        assertArrayEquals(pattern1, Arrays.copyOfRange(fileContent, pattern1.length + pattern2.length, fileContent.length));
    }

    @Test
    void testWriteSingleByte() throws Exception {

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(new byte[]{42});
        }

        byte[] content = Files.readAllBytes(testFile);
        assertEquals(1, content.length);
        assertEquals(42, content[0]);
    }

    @Test
    void testManySmallWrites() throws Exception {

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            for (int i = 0; i < 1000; i++) {
                writer.write(new byte[]{(byte) i});
            }
        }

        byte[] content = Files.readAllBytes(testFile);
        assertEquals(1000, content.length);
        for (int i = 0; i < 1000; i++) {
            assertEquals((byte) i, content[i]);
        }
    }

    @Test
    void testVeryLargeFile() throws Exception {

        // Тест для проверки работы с большими файлами (>2GB)
        // Можно уменьшить размер для обычного тестирования
        final int chunkSize = 1024 * 1024; // 1MB
        final int chunks = 10; // 10MB всего

        byte[] chunk = new byte[chunkSize];
        Arrays.fill(chunk, (byte) 7);

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            for (int i = 0; i < chunks; i++) {
                writer.write(chunk);
            }
        }

        assertEquals((long) chunkSize * chunks, Files.size(testFile));
    }

    @Test
    void testConstructorWithInvalidPath() {

        Path invalidPath = Path.of("/invalid/path/to/file.bin");
        assertThrows(IOException.class, () -> new BatchFileWriterCallback<>(invalidPath));
    }

    @Test
    void testWriteToReadOnlyFile(@TempDir Path tempDir) throws Exception {

        Path readOnlyFile = tempDir.resolve("readonly.bin");

        // Создаем файл и устанавливаем права только для чтения
        Files.createFile(readOnlyFile);
        assertTrue(readOnlyFile.toFile().setReadOnly());

        // Проверяем, что файл действительно только для чтения
        assertFalse(Files.isWritable(readOnlyFile));

        // Проверяем, что попытка записи вызывает IOException
        IOException exception = assertThrows(IOException.class, () -> {
            try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(readOnlyFile)) {
                writer.write(new byte[100]);
            }
        });

        // Проверяем сообщение об ошибке
        assertTrue(exception.getMessage().contains("denied") ||
                exception.getMessage().contains("read-only") ||
                exception instanceof AccessDeniedException);
    }

    @Test
    void testWriteToReadOnlyFileNio(@TempDir Path tempDir) throws Exception {

        Path readOnlyFile = tempDir.resolve("readonly.bin");
        Files.createFile(readOnlyFile);

        // Устанавливаем атрибуты только для чтения (работает на Unix и Windows)
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.OTHERS_READ);

        try {
            Files.setPosixFilePermissions(readOnlyFile, perms);
        } catch (UnsupportedOperationException e) {
            // Если файловая система не поддерживает POSIX (например, Windows)
            assumeTrue(false, "POSIX permissions not supported");
        }

        // Проверяем, что файл действительно только для чтения
        assertFalse(Files.isWritable(readOnlyFile));

        // Проверяем обработку ошибки
        assertThrows(IOException.class, () -> {
            try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(readOnlyFile)) {
                writer.write(new byte[100]);
            }
        });
    }

    @Test
    void testLargeDataWrittenImmediately2() throws Exception {

        byte[] largeData = new byte[MIN_BATCH_SIZE * 2];
        Arrays.fill(largeData, (byte) 2);

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(largeData);
            // Большие данные должны записаться сразу
            assertEquals(largeData.length, Files.size(testFile));

            // Проверяем, что данные действительно записались
            byte[] writtenData = Files.readAllBytes(testFile);
            assertArrayEquals(largeData, writtenData);

            // Проверяем, что буфер пуст после записи больших данных
            writer.write(new byte[10]); // Маленькие данные после больших
            assertEquals(largeData.length, Files.size(testFile)); // Еще не записались
        }

        // После закрытия должны записаться и маленькие данные
        assertEquals(largeData.length + 10, Files.size(testFile));
    }

    @Test
    void testVeryLargeDataMultipleTimes() throws Exception {

        byte[] largeData1 = new byte[MIN_BATCH_SIZE * 3];
        Arrays.fill(largeData1, (byte) 1);

        byte[] largeData2 = new byte[MIN_BATCH_SIZE * 4];
        Arrays.fill(largeData2, (byte) 2);

        byte[] smallData = new byte[100];
        Arrays.fill(smallData, (byte) 3);

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(largeData1);
            assertEquals(largeData1.length, Files.size(testFile));

            writer.write(smallData);
            assertEquals(largeData1.length, Files.size(testFile)); // Еще в буфере

            writer.write(largeData2);
            assertEquals(largeData1.length + smallData.length + largeData2.length, Files.size(testFile));
        }
    }

    @Test
    void testMixedLargeAndSmallData() throws Exception {

        byte[] small1 = new byte[100];
        byte[] large = new byte[MIN_BATCH_SIZE * 2];
        byte[] small2 = new byte[200];

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(small1);
            assertEquals(0, Files.size(testFile));

            writer.write(large);
            assertEquals(small1.length + large.length, Files.size(testFile));

            writer.write(small2);
            assertEquals(small1.length + large.length, Files.size(testFile)); // Еще в буфере
        }

        assertEquals(small1.length + large.length + small2.length, Files.size(testFile));
    }

    @Test
    void testWriteAfterCloseThrowsException2() throws Exception {

        BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile);
        writer.close();

        IOException exception = assertThrows(IOException.class,
                () -> writer.write(new byte[1]));

        assertEquals("Writer is closed", exception.getMessage());
    }

    @Test
    void testDoubleCloseIsSafe() throws Exception {

        BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile);
        writer.close();
        assertDoesNotThrow(writer::close); // Повторное закрытие не должно бросать исключение
    }


    @Test
    void testAutoFlushWhenBatchSizeReached2() throws Exception {

        // Первая порция данных - чуть меньше MIN_BATCH_SIZE
        byte[] data1 = new byte[MIN_BATCH_SIZE - 100];
        Arrays.fill(data1, (byte) 1);

        // Вторая порция - 200 байт, что вместе превысит MIN_BATCH_SIZE
        byte[] data2 = new byte[200];
        Arrays.fill(data2, (byte) 2);

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            // Первая запись - данные остаются в буфере
            writer.write(data1);
            assertEquals(0, Files.size(testFile), "Данные не должны быть записаны сразу");

            // Вторая запись - вызывает авто-flush
            writer.write(data2);

            // Проверяем что ВСЕ данные записаны (и data1 и data2)
            byte[] fileContent = Files.readAllBytes(testFile);
            assertEquals(data1.length + data2.length, fileContent.length);

            // Проверяем содержимое первых data1.length байт
            byte[] firstPart = Arrays.copyOfRange(fileContent, 0, data1.length);
            assertArrayEquals(data1, firstPart);

            // Проверяем содержимое следующих data2.length байт
            byte[] secondPart = Arrays.copyOfRange(fileContent, data1.length, fileContent.length);
            assertArrayEquals(data2, secondPart);
        }
    }

    @Test
    void testExactBatchSize() throws Exception {

        byte[] exactSizeData = new byte[MIN_BATCH_SIZE];
        Arrays.fill(exactSizeData, (byte) 3);

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(exactSizeData);
            assertEquals(MIN_BATCH_SIZE, Files.size(testFile));
        }
    }

    @Test
    void testBoundaryConditions() throws Exception {

        // Данные ровно MIN_BATCH_SIZE
        byte[] exactSize = new byte[MIN_BATCH_SIZE];
        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(exactSize);
            assertEquals(MIN_BATCH_SIZE, Files.size(testFile));
        }

        // Данные на 1 байт меньше
        byte[] oneLess = new byte[MIN_BATCH_SIZE - 1];
        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(oneLess);
            assertEquals(0, Files.size(testFile)); // Не должно записаться
            writer.write(new byte[1]); // Дописываем 1 байт
            assertEquals(MIN_BATCH_SIZE, Files.size(testFile));
        }
    }

    @Test
    void testChunkSizes() throws Exception {

        // Проверяем разные размеры чанков
        int[] sizes = {1, 100, 511, 1023, 2047, 4095, 4096, 4097};
        for (int size : sizes) {
            Path tempFile = testFile.resolveSibling("chunk_" + size + ".bin");
            try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(tempFile)) {
                writer.write(new byte[size]);
                if (size >= MIN_BATCH_SIZE) {
                    assertEquals(size, Files.size(tempFile));
                } else {
                    assertEquals(0, Files.size(tempFile));
                }
            }
        }
    }

    @Test
    void testExactBatchSizeWrite() throws Exception {

        byte[] exactSizeData = new byte[MIN_BATCH_SIZE];
        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(exactSizeData);
            assertEquals(MIN_BATCH_SIZE, Files.size(testFile));
        }
    }

    @Test
    void testOneByteOverBatchSize() throws Exception {

        byte[] exactSize = new byte[MIN_BATCH_SIZE];
        byte[] oneByte = new byte[1];

        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.write(exactSize);
            assertEquals(MIN_BATCH_SIZE, Files.size(testFile));

            writer.write(oneByte);
        }
        assertEquals(MIN_BATCH_SIZE + 1, Files.size(testFile));
    }

    @Test
    void callback() throws Exception {
        byte[] b1 = new byte[MIN_BATCH_SIZE];
        byte[] b2 = new byte[MIN_BATCH_SIZE];
        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.setOnFlush(callbacks -> UtilLog.printInfo(BatchFileWriterCallbackTest.class, callbacks));
            writer.write(b1, new Callback());
            assertEquals(MIN_BATCH_SIZE, Files.size(testFile));
            writer.write(b2, new Callback());
            assertEquals(MIN_BATCH_SIZE * 2, Files.size(testFile));
        }
    }

    @Test
    void testTiming() throws Exception {
        long time = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger(0);
        try (BatchFileWriterCallback<Callback> writer = new BatchFileWriterCallback<>(testFile)) {
            writer.setOnFlush(callbacks -> counter.addAndGet(callbacks.size()));
            for (int i = 0; i < 400_000; i++) {
                writer.write("Hello world".getBytes(StandardCharsets.UTF_8), new Callback());
            }
        }
        Assertions.assertEquals(400_000, counter.get());
        System.out.println("timing: " + (System.currentTimeMillis() - time) + "; callback: " + counter.get());
    }

}