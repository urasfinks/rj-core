package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AsyncFileWriterTest {

    private AsyncFileWriter<TestElement> writer;
    private final ConcurrentLinkedDeque<TestElement> outputQueue = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean run = new AtomicBoolean(true);

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runSpring();
        App.get(ServiceProperty.class).set("App.AsyncFileWriter.test.file.path", "tmp.dat");
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @BeforeEach
    void setUp() {
        run.set(true);
        writer = new AsyncFileWriter<>("test", App.context, outputQueue::addAll);
        writer.run();
    }

    @AfterEach
    void tearDown() {
        run.set(false);
        outputQueue.clear();
        if (writer != null) {
            writer.shutdown();
            try {
                Files.deleteIfExists(Paths.get(writer.getRepositoryProperty().getFilePath()));
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    void testSingleWriteAndFlush() throws Exception {
        TestElement element = new TestElement("Hello".getBytes());
        writer.writeAsync(element);

        TestElement element2 = new TestElement("Hello".getBytes());
        Assertions.assertEquals(5, element2.getBytes().length);
        writer.writeAsync(element2);

        writer.flush(run);

        assertEquals(2, outputQueue.size());
        assertEquals(0, element.getPosition());
        assertEquals(5, element2.getPosition());

        byte[] bytes = Files.readAllBytes(Paths.get(writer.getRepositoryProperty().getFilePath()));
        assertArrayEquals("HelloHello".getBytes(), bytes);
    }

    @Test
    void testMultipleWritesAndFlush() throws Exception {
        List<TestElement> elements = List.of(
                new TestElement("abc".getBytes()),
                new TestElement("1234".getBytes()),
                new TestElement("XYZ".getBytes())
        );

        for (TestElement el : elements) {
            writer.writeAsync(el);
        }

        writer.flush(run);

        assertEquals(3, outputQueue.size());

        long expectedPos = 0;
        for (TestElement el : elements) {
            assertEquals(expectedPos, el.getPosition());
            expectedPos += el.getBytes().length;
        }

        byte[] bytes = Files.readAllBytes(Paths.get(writer.getRepositoryProperty().getFilePath()));
        assertArrayEquals("abc1234XYZ".getBytes(), bytes);
    }

    @Test
    void testPositionIsUpdatedCorrectlyAfterFlushes() throws Exception {
        TestElement e1 = new TestElement("A".repeat(2048).getBytes()); // 2KB
        TestElement e2 = new TestElement("B".repeat(2048).getBytes()); // 2KB

        writer.writeAsync(e1);
        writer.writeAsync(e2);

        writer.flush(run); // should trigger batch flush due to 4KB size

        TestElement e3 = new TestElement("C".repeat(100).getBytes());
        writer.writeAsync(e3);
        writer.flush(run);

        assertEquals(3, outputQueue.size());

        assertEquals(0, e1.getPosition());
        assertEquals(2048, e2.getPosition());
        assertEquals(4096, e3.getPosition());

        byte[] bytes = Files.readAllBytes(Paths.get(writer.getRepositoryProperty().getFilePath()));
        assertEquals(4096 + 100, bytes.length);
    }

    @Test
    void testShutdownFlushesRemainingData() throws Exception {
        TestElement e1 = new TestElement("abc".getBytes());
        TestElement e2 = new TestElement("def".getBytes());

        writer.writeAsync(e1);
        writer.writeAsync(e2);

        writer.shutdown(); // should flush pending

        assertEquals(2, outputQueue.size());

        byte[] bytes = Files.readAllBytes(Paths.get(writer.getRepositoryProperty().getFilePath()));
        assertArrayEquals("abcdef".getBytes(), bytes);
    }

    @Test
    void testWriteAfterShutdownThrowsException() {
        writer.shutdown();
        TestElement e = new TestElement("error".getBytes());
        IOException ex = assertThrows(IOException.class, () -> writer.writeAsync(e));
        assertEquals("Writer is closed", ex.getMessage());
    }

    @Test
    void multiThread() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        long now = System.currentTimeMillis();
        long initialDelay = 1000 - (now % 1000);
        scheduler.scheduleAtFixedRate(
                () -> {
                    if (run.get()) {
                        try {
                            writer.flush(run);
                            UtilLog.printInfo(writer.flushAndGetStatistic(run));
                        } catch (IOException e) {
                            App.error(e);
                        }
                    }
                },
                initialDelay,
                1000,
                TimeUnit.MILLISECONDS
        );
        int c = 1_000_000;
        for (int i = 0; i < 4; i++) {
            new Thread(() -> {
                try {
                    for (int y = 0; y < c; y++) {
                        writer.writeAsync(new TestElement("Hello".getBytes()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        Util.testSleepMs(3_000);
        long start = System.currentTimeMillis();
        while (true) {
            if ((4 * c) == outputQueue.size()) {
                break;
            }
            if (System.currentTimeMillis() - start > 11_000) {
                break;
            }
            Thread.onSpinWait();
        }
        long fin = System.currentTimeMillis() - start;
        run.set(false);
        //Assertions.assertEquals(4 * c, writer.getOutputQueue().size());
        UtilLog.printInfo(new HashMapBuilder<String, Object>()
                .append("sizeMb", ((float) writer.getPosition().get()) / 1024 / 1024)
                .append("time", fin)
        );
        Assertions.assertTrue(fin < 50);
    }

    // Реализация тестового элемента
    public static class TestElement extends AbstractAsyncFileWriterElement {
        private final byte[] data;
        @Getter
        private long position = -1;

        public TestElement(byte[] data) {
            this.data = data;
        }

        @Override
        public byte[] getBytes() {
            return data;
        }

        @Override
        public void setPosition(long pos) {
            this.position = pos;
        }

    }
}