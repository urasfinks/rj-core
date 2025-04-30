package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaitQueueTest {
    private WaitQueue<SimpleTask> queue;

    @BeforeEach
    void setUp() {
        queue = new WaitQueue<>();
    }

    @Test
    void testPollSimpleTasks() {
        queue.getMainQueue().add(new SimpleTask("1", false));
        queue.getMainQueue().add(new SimpleTask("2", false));
        queue.getMainQueue().add(new SimpleTask("3", true)); // wait элемент

        List<SimpleTask> polled = queue.poll();
        assertEquals("1", polled.getFirst().getNs());
        assertEquals("2", polled.getLast().getNs());

        assertEquals(0, queue.getMainQueue().size());
        assertEquals(2, polled.size(), "Should poll 2 tasks before wait task");
        assertEquals(2, queue.getPolledQueue().size(), "queuePolled should have 2 tasks");

    }

    @Test
    void testTwoBatchPollSeparatedByWait() {
        // Подготовка очереди
        queue.getMainQueue().add(new SimpleTask("task1", false));
        queue.getMainQueue().add(new SimpleTask("task2", false));
        queue.getMainQueue().add(new SimpleTask("wait1", true)); // Wait элемент
        queue.getMainQueue().add(new SimpleTask("task3", false));
        queue.getMainQueue().add(new SimpleTask("task4", false));

        // Первый poll — до wait
        List<SimpleTask> firstBatch = queue.poll();
        assertEquals(2, firstBatch.size(), "First batch should have 2 tasks");
        assertEquals("task1", firstBatch.get(0).getNs());
        assertEquals("task2", firstBatch.get(1).getNs());

        assertEquals(2, queue.getMainQueue().size());

        // Пока не закоммитили первую пачку — второй poll ничего не должен отдать
        List<SimpleTask> pollWhileFirstBatchNotCommitted = queue.poll();
        assertEquals(0, pollWhileFirstBatchNotCommitted.size(), "Should not poll second batch before first batch commit");

        // Коммитим первую пачку
        queue.commit(firstBatch.get(0));
        queue.commit(firstBatch.get(1));

        // Теперь можно вызывать poll снова — должны получить вторую пачку
        List<SimpleTask> secondBatch = queue.poll();
        assertEquals(2, secondBatch.size(), "Second batch should have 2 tasks after first batch is committed");
        assertEquals("task3", secondBatch.get(0).getNs());
        assertEquals("task4", secondBatch.get(1).getNs());
    }

    @Test
    void testCommitTasks() {
        queue.getMainQueue().add(new SimpleTask("1", false));
        queue.getMainQueue().add(new SimpleTask("2", false));

        List<SimpleTask> polled = queue.poll();
        assertEquals(2, polled.size());

        queue.commit(polled.get(0));
        assertEquals(1, queue.getPolledQueue().size(), "One task should remain after one commit");

        queue.commit(polled.get(1));
        assertEquals(0, queue.getPolledQueue().size(), "All tasks should be committed");
    }

    @Test
    void testPollAfterFullCommit() {
        queue.getMainQueue().add(new SimpleTask("1", false));
        queue.getMainQueue().add(new SimpleTask("2", false));

        List<SimpleTask> firstPoll = queue.poll();
        assertEquals(2, firstPoll.size());

        queue.commit(firstPoll.get(0));
        queue.commit(firstPoll.get(1));

        // После полного коммита можно снова poll
        queue.getMainQueue().add(new SimpleTask("3", false));
        List<SimpleTask> secondPoll = queue.poll();

        assertEquals(1, secondPoll.size(), "Should be able to poll again after full commit");
        assertEquals("3", secondPoll.getFirst().getNs());
    }

    @Test
    void testSkipUntil() {
        queue.getMainQueue().add(new SimpleTask("1", false));
        queue.getMainQueue().add(new SimpleTask("2", false));
        queue.getMainQueue().add(new SimpleTask("3", false));
        queue.getMainQueue().add(new SimpleTask("4", false));

        queue.skipUntil("3");

        assertEquals(2, queue.getMainQueue().size(), "After goTo 2 tasks should remain");
        assert queue.getMainQueue().peekFirst() != null;
        assertEquals("3", queue.getMainQueue().peekFirst().getNs(), "First remaining task should be 3");
    }

    @Getter
    @Setter
    static class SimpleTask implements WaitQueueElement {

        private final String ns;
        private final boolean wait;

        public SimpleTask(String ns, boolean wait) {
            this.ns = ns;
            this.wait = wait;
        }

    }

}