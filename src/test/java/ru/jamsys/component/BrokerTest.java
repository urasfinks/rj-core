package ru.jamsys.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.component.item.BrokerQueue;
import ru.jamsys.statistic.TaskStatistic;
import ru.jamsys.statistic.TimeControllerImpl;
import ru.jamsys.statistic.TimeEnvelope;

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
        Broker<XTest> broker = App.context.getBean(Broker.class);
        BrokerQueue b = broker.getItem(XTest.class.getSimpleName());

        b.setCyclical(false);
        b.setSizeQueue(10);
        b.setSizeTail(3);

        for (int i = 0; i < 10; i++) {
            b.addTest(new XTest(i));
        }

        Assertions.assertEquals(10, b.getSize(), "#1");

        TimeEnvelope<XTest> t = b.pollFirst();
        Assertions.assertEquals(0, t.getValue().x, "#2");
        Assertions.assertEquals(9, b.getSize(), "#3");

        TimeEnvelope<XTest> t2 = b.pollLast();
        Assertions.assertEquals(9, t2.getValue().x, "#4");
        Assertions.assertEquals(8, b.getSize(), "#5");

        try {
            b.addTest(new XTest(11));
            b.addTest(new XTest(12));
            b.addTest(new XTest(13));
            b.addTest(new XTest(14));
            Assertions.fail("#6");
        } catch (Exception e) {
            Assertions.assertTrue(true, "#7");
        }
        List<XTest> tail = b.getTail(null);
        Assertions.assertEquals("[XTest{x=11}, XTest{x=12}, XTest{x=13}]", tail.toString(), "#8");

        List<XTest> cloneQueue = b.getCloneQueue(null);
        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}]", cloneQueue.toString(), "#9");
        b.reset();
        Assertions.assertEquals("[]", b.getCloneQueue(null).toString(), "#10");
    }

    @Test
    void testCyclic() throws Exception {
        Broker<XTest> broker = App.context.getBean(Broker.class);
        BrokerQueue b = broker.getItem(XTest.class.getSimpleName());

        b.setCyclical(true);
        b.setSizeQueue(10);
        b.setSizeTail(3);

        for (int i = 0; i < 10; i++) {
            b.addTest(new XTest(i));
        }

        Assertions.assertEquals("[XTest{x=0}, XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=9}]", b.getCloneQueue(null).toString());

        Assertions.assertEquals(10, b.getSize());

        TimeEnvelope<XTest> t = b.pollFirst();

        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=9}]", b.getCloneQueue(null).toString());

        Assertions.assertEquals(0, t.getValue().x);
        Assertions.assertEquals(9, b.getSize());

        TimeEnvelope<XTest> t2 = b.pollLast();

        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}]", b.getCloneQueue(null).toString());

        Assertions.assertEquals(9, t2.getValue().x);
        Assertions.assertEquals(8, b.getSize());

        try {
            b.addTest(new XTest(11));
            Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}]", b.getCloneQueue(null).toString());
            b.addTest(new XTest(12));
            Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}]", b.getCloneQueue(null).toString());
            b.addTest(new XTest(13));
            Assertions.assertEquals("[XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}]", b.getCloneQueue(null).toString());
            b.addTest(new XTest(14));
            Assertions.assertEquals("[XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}, XTest{x=14}]", b.getCloneQueue(null).toString());
            Assertions.assertTrue(true, "#6");
        } catch (Exception e) {
            Assertions.fail("#7");
        }

        List<XTest> tail = b.getTail(null);
        Assertions.assertEquals("[XTest{x=12}, XTest{x=13}, XTest{x=14}]", tail.toString(), "#8");

        Assertions.assertEquals("[XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}, XTest{x=14}]", b.getCloneQueue(null).toString());
        b.reset();
        Assertions.assertEquals("[]", b.getCloneQueue(null).toString(), "#10");
    }

    @Test
    void testReference() throws Exception {
        Broker<TaskStatistic> broker = App.context.getBean(Broker.class);
        BrokerQueue<TaskStatistic> queue = broker.getItem(TaskStatistic.class.getSimpleName());
        TaskStatistic obj = new TaskStatistic(null, null);
        TimeEnvelope<TaskStatistic> o1 = queue.addTest(obj);
        List<TaskStatistic> cloneQueue = queue.getCloneQueue(null);
        Assertions.assertEquals(obj.hashCode(), cloneQueue.get(0).hashCode(), "#1");
        queue.remove(o1);
        Assertions.assertEquals(0, queue.getSize(), "#1");
        queue.reset();
    }

    @Test
    void testReference2() throws Exception {
        AtomicBoolean isRun = new AtomicBoolean(true);
        Broker<TaskStatistic> broker = App.context.getBean(Broker.class);
        BrokerQueue<TaskStatistic> queue = broker.getItem(TaskStatistic.class.getSimpleName());
        TaskStatistic obj = new TaskStatistic(null, null);
        TaskStatistic obj2 = new TaskStatistic(null, null);
        TimeEnvelope<TaskStatistic> o1 = queue.addTest(obj);
        TimeEnvelope<TaskStatistic> o2 = queue.addTest(obj2);
        List<TaskStatistic> cloneQueue = queue.getCloneQueue(isRun);
        Assertions.assertEquals(obj.hashCode(), cloneQueue.get(0).hashCode(), "#1");
        Assertions.assertEquals(obj2.hashCode(), cloneQueue.get(1).hashCode(), "#2");
        queue.remove(o1);
        Assertions.assertEquals(1, queue.getSize(), "#3");
        queue.remove(o2);
        Assertions.assertEquals(0, queue.getSize(), "#4");
        queue.reset();
    }

    @Test
    void testMaxInputTps() {
        Broker<TaskStatistic> broker = App.context.getBean(Broker.class);
        BrokerQueue<TaskStatistic> queue = broker.getItem(TaskStatistic.class.getSimpleName());
        queue.setMaxTpsInput(1);
        TaskStatistic obj = new TaskStatistic(null, null);
        try {
            queue.addTest(obj);
        } catch (Exception e) {
            Assertions.fail();
        }
        try {
            queue.addTest(obj);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(true);
        }

        queue.reset();
    }

    static class XTest extends TimeControllerImpl {
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