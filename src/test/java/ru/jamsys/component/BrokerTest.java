package ru.jamsys.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.broker.BrokerQueue;
import ru.jamsys.broker.Queue;
import ru.jamsys.task.TaskHandlerStatistic;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class BrokerTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.context = SpringApplication.run(App.class, args);
    }

    @Test
    void testLiner() throws Exception {
        Broker broker = App.context.getBean(Broker.class);
        BrokerQueue b = (BrokerQueue) broker.get(XTest.class.getSimpleName());

        b.setCyclical(false);
        b.setSizeQueue(10);
        b.setSizeTail(3);

        for (int i = 0; i < 10; i++) {
            b.add(new XTest(i));
        }

        Assertions.assertEquals(10, b.getSize(), "#1");

        XTest t = (XTest) b.pollFirst();
        Assertions.assertEquals(0, t.x, "#2");
        Assertions.assertEquals(9, b.getSize(), "#3");

        XTest t2 = (XTest) b.pollLast();
        Assertions.assertEquals(9, t2.x, "#4");
        Assertions.assertEquals(8, b.getSize(), "#5");

        try {
            b.add(new XTest(11));
            b.add(new XTest(12));
            b.add(new XTest(13));
            b.add(new XTest(14));
            Assertions.fail("#6");
        } catch (Exception e) {
            Assertions.assertTrue(true, "#7");
        }
        List<XTest> tail = b.getTail(null);
        Assertions.assertEquals("[XTest{x=11}, XTest{x=12}, XTest{x=13}]", tail.toString(), "#8");
        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}]", b.getCloneQueue(null).toString(), "#9");
        b.reset();
        Assertions.assertEquals("[]", b.getCloneQueue(null).toString(), "#10");
    }

    @Test
    void testCyclic() throws Exception {
        Broker broker = App.context.getBean(Broker.class);
        BrokerQueue b = (BrokerQueue) broker.get(XTest.class.getSimpleName());

        b.setCyclical(true);
        b.setSizeQueue(10);
        b.setSizeTail(3);

        for (int i = 0; i < 10; i++) {
            b.add(new XTest(i));
        }

        Assertions.assertEquals(10, b.getSize(), "#1");

        XTest t = (XTest) b.pollFirst();
        Assertions.assertEquals(0, t.x, "#2");
        Assertions.assertEquals(9, b.getSize(), "#3");

        XTest t2 = (XTest) b.pollLast();
        Assertions.assertEquals(9, t2.x, "#4");
        Assertions.assertEquals(8, b.getSize(), "#5");

        try {
            b.add(new XTest(11));
            b.add(new XTest(12));
            b.add(new XTest(13));
            b.add(new XTest(14));
            Assertions.assertTrue(true, "#6");
        } catch (Exception e) {
            Assertions.fail("#7");
        }

        List<XTest> tail = b.getTail(null);
        Assertions.assertEquals("[XTest{x=12}, XTest{x=13}, XTest{x=14}]", tail.toString(), "#8");
        Assertions.assertEquals("[XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}, XTest{x=14}]", b.getCloneQueue(null).toString(), "#9");
        b.reset();
        Assertions.assertEquals("[]", b.getCloneQueue(null).toString(), "#10");
    }

    @Test
    void testReference() throws Exception {
        Broker broker = App.context.getBean(Broker.class);
        Queue<TaskHandlerStatistic> queue = broker.get(TaskHandlerStatistic.class.getSimpleName());
        TaskHandlerStatistic obj = new TaskHandlerStatistic(Thread.currentThread(), null, null);
        queue.add(obj);
        List<TaskHandlerStatistic> cloneQueue = queue.getCloneQueue(null);
        Assertions.assertEquals(obj.hashCode(), cloneQueue.get(0).hashCode(), "#1");
        queue.remove(obj);
        Assertions.assertEquals(0, queue.getSize(), "#1");
        queue.reset();
    }

    @Test
    void testReference2() throws Exception {
        AtomicBoolean isRun = new AtomicBoolean(true);
        Broker broker = App.context.getBean(Broker.class);
        Queue<TaskHandlerStatistic> queue = broker.get(TaskHandlerStatistic.class.getSimpleName());
        TaskHandlerStatistic obj = new TaskHandlerStatistic(Thread.currentThread(), null, null);
        TaskHandlerStatistic obj2 = new TaskHandlerStatistic(Thread.currentThread(), null, null);
        queue.add(obj);
        queue.add(obj2);
        List<TaskHandlerStatistic> cloneQueue = queue.getCloneQueue(isRun);
        Assertions.assertEquals(obj.hashCode(), cloneQueue.get(0).hashCode(), "#1");
        Assertions.assertEquals(obj2.hashCode(), cloneQueue.get(1).hashCode(), "#2");
        queue.remove(obj);
        Assertions.assertEquals(1, queue.getSize(), "#3");
        queue.remove(obj2);
        Assertions.assertEquals(0, queue.getSize(), "#4");
        queue.reset();
    }

    static class XTest implements BrokerCollectible {
        final int x;

        XTest(int x) {
            this.x = x;
        }

        @Override
        public String toString() {
            return "XTest{" +
                    "x=" + x +
                    '}';
        }
    }
}