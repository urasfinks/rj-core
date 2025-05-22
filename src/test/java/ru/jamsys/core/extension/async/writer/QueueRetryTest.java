package ru.jamsys.core.extension.async.writer;

import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.log.DataHeader;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class QueueRetryTest {

    AtomicBoolean threadRun = new AtomicBoolean(true);

    private QueueRetry queueRetry;

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runSpring();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @BeforeEach
    public void setup() {
        queueRetry = new QueueRetry("test-key", false);
    }

    @Test
    void test() {
        long curTimeMs = 1709734264056L; //2024-03-06T17:11:04.056

        QueueRetry test = new QueueRetry("test", true);

        test.add(0, null, "Hello");
        Assertions.assertEquals(1, test.size());
        Assertions.assertTrue(test.getExpirationListConfiguration().get().isEmpty());

        DataReadWrite dataReadWrite = test.pollLast(1_000, curTimeMs);
        Assertions.assertEquals(1, test.size());
        Assertions.assertFalse(test.getExpirationListConfiguration().get().isEmpty());

        test.getExpirationListConfiguration().get().helper(threadRun, curTimeMs + 2000);

        // Должно произойти протухание и обратно в queue вставиться
        Assertions.assertEquals(1, test.size());
        Assertions.assertTrue(test.getExpirationListConfiguration().get().isEmpty());

        DataReadWrite dataReadWrite2 = test.pollLast(1_500, curTimeMs);
        // Это 2 одинаковых объекта
        Assertions.assertEquals(dataReadWrite, dataReadWrite2);
        Assertions.assertEquals(1, test.size());
        Assertions.assertFalse(test.getExpirationListConfiguration().get().isEmpty());
        Assertions.assertFalse(test.isProcessed());

        // Теперь делаю человеческое удаление
        test.remove(dataReadWrite2.getPosition());
        test.getExpirationListConfiguration().get().helper(threadRun, curTimeMs + 2000);

        // Теперь ничего не должно нигде остаться
        Assertions.assertEquals(0, test.size());
        Assertions.assertTrue(test.getExpirationListConfiguration().get().isEmpty());
        Assertions.assertTrue(test.isProcessed());

    }

    @Test
    public void testAddAndPoll() {
        DataReadWrite dataReadWrite = new DataReadWrite(1L, new byte[]{1, 2, 3}, null);
        queueRetry.add(dataReadWrite);

        DataReadWrite polled = queueRetry.pollLast(1000);
        Assertions.assertNotNull(polled);
        Assertions.assertEquals(1L, polled.getPosition());
    }

    @Test
    public void testDuplicateAdd() {
        DataReadWrite dataReadWrite = new DataReadWrite(2L, new byte[]{9, 8}, null);
        queueRetry.add(dataReadWrite);
        queueRetry.add(dataReadWrite); // should log error, not throw
        Assertions.assertEquals(1, queueRetry.size());
    }

    @Test
    public void testRemoveBeforePoll() {
        DataReadWrite dataReadWrite = new DataReadWrite(3L, new byte[]{7}, null);
        queueRetry.add(dataReadWrite);
        queueRetry.remove(dataReadWrite.getPosition());

        DataReadWrite polled = queueRetry.pollLast(1000);
        Assertions.assertNull(polled);
    }

    @Test
    public void testIsProcessed() {
        Assertions.assertFalse(queueRetry.isProcessed());
        queueRetry.setFinishState(true);
        Assertions.assertTrue(queueRetry.isProcessed());
    }

    @Test
    public void testGetForUnitTest() {
        DataReadWrite dataReadWrite = new DataReadWrite(4L, new byte[]{5}, null);
        queueRetry.add(dataReadWrite);
        DataReadWrite fetched = queueRetry.getForUnitTest(4L);
        Assertions.assertNotNull(fetched);
        Assertions.assertEquals(dataReadWrite, fetched);
    }

    @Test
    public void testFlushStatistics() {
        DataReadWrite dataReadWrite = new DataReadWrite(5L, new byte[]{1}, null);
        queueRetry.add(dataReadWrite);
        List<DataHeader> stats = queueRetry.flushAndGetStatistic(new AtomicBoolean(true));
        Assertions.assertFalse(stats.isEmpty());
    }

}