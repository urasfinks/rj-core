package ru.jamsys.core.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.api.BrokerManager;
import ru.jamsys.core.component.item.Broker;
import ru.jamsys.core.statistic.time.TimeControllerMs;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class BrokerTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        if (App.context == null) {
            App.context = SpringApplication.run(App.class, args);
        }
        //App.main(args);
    }

    @Test
    void testLiner() throws Exception {
        @SuppressWarnings("unchecked")
        BrokerManager<XTest> broker = App.context.getBean(BrokerManager.class);
        Broker<XTest> b = broker.get(XTest.class.getSimpleName());

        b.setCyclical(false);
        b.setSizeQueue(10);
        b.setSizeQueueTail(3);

        for (int i = 0; i < 10; i++) {
            b.add(new XTest(i), 6_000L);
        }

        Assertions.assertEquals(10, b.size(), "#1");

        TimeEnvelopeMs<XTest> t = b.pollFirst();
        Assertions.assertEquals(0, t.getValue().x, "#2");
        Assertions.assertEquals(9, b.size(), "#3");

        TimeEnvelopeMs<XTest> t2 = b.pollLast();
        Assertions.assertEquals(9, t2.getValue().x, "#4");
        Assertions.assertEquals(8, b.size(), "#5");

        try {
            b.add(new XTest(11), 6_000L);
            b.add(new XTest(12), 6_000L);
            b.add(new XTest(13), 6_000L);
            b.add(new XTest(14), 6_000L);
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
        @SuppressWarnings("unchecked")
        BrokerManager<XTest> broker = App.context.getBean(BrokerManager.class);
        Broker<XTest> b = broker.get(XTest.class.getSimpleName());

        b.setCyclical(true);
        b.setSizeQueue(10);
        b.setSizeQueueTail(3);

        for (int i = 0; i < 10; i++) {
            b.add(new XTest(i), 6_000L);
        }

        Assertions.assertEquals("[XTest{x=0}, XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=9}]", b.getCloneQueue(null).toString());

        Assertions.assertEquals(10, b.size());

        TimeEnvelopeMs<XTest> t = b.pollFirst();

        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=9}]", b.getCloneQueue(null).toString());

        Assertions.assertEquals(0, t.getValue().x);
        Assertions.assertEquals(9, b.size());

        TimeEnvelopeMs<XTest> t2 = b.pollLast();

        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}]", b.getCloneQueue(null).toString());

        Assertions.assertEquals(9, t2.getValue().x);
        Assertions.assertEquals(8, b.size());

        try {
            b.add(new XTest(11), 6_000L);
            Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}]", b.getCloneQueue(null).toString());
            b.add(new XTest(12), 6_000L);
            Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}]", b.getCloneQueue(null).toString());
            b.add(new XTest(13), 6_000L);
            Assertions.assertEquals("[XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}]", b.getCloneQueue(null).toString());
            b.add(new XTest(14), 6_000L);
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
        @SuppressWarnings("unchecked")
        BrokerManager<XTest> broker = App.context.getBean(BrokerManager.class);
        Broker<XTest> queue = broker.get(TimeControllerMs.class.getSimpleName());
        XTest obj = new XTest(1);
        TimeEnvelopeMs<XTest> o1 = queue.add(obj, 6_000L);
        List<XTest> cloneQueue = queue.getCloneQueue(null);
        Assertions.assertEquals(obj.hashCode(), cloneQueue.getFirst().hashCode(), "#1");
        queue.remove(o1);
        Assertions.assertEquals(0, queue.size(), "#1");
        queue.reset();
    }

    @Test
    void testReference2() throws Exception {
        AtomicBoolean isRun = new AtomicBoolean(true);
        @SuppressWarnings("unchecked")
        BrokerManager<XTest> broker = App.context.getBean(BrokerManager.class);
        Broker<XTest> queue = broker.get(XTest.class.getSimpleName());
        XTest obj = new XTest(1);
        XTest obj2 = new XTest(2);
        TimeEnvelopeMs<XTest> o1 = queue.add(obj, 6_000L);
        TimeEnvelopeMs<XTest> o2 = queue.add(obj2, 6_000L);
        List<XTest> cloneQueue = queue.getCloneQueue(isRun);
        Assertions.assertEquals(obj.hashCode(), cloneQueue.get(0).hashCode(), "#1");
        Assertions.assertEquals(obj2.hashCode(), cloneQueue.get(1).hashCode(), "#2");
        queue.remove(o1);
        Assertions.assertEquals(1, queue.size(), "#3");
        queue.remove(o2);
        Assertions.assertEquals(0, queue.size(), "#4");
        queue.reset();
    }

    @Test
    void testMaxInputTps() {
        @SuppressWarnings("unchecked")
        BrokerManager<XTest> broker = App.context.getBean(BrokerManager.class);
        Broker<XTest> queue = broker.get(XTest.class.getSimpleName());
        queue.setMaxTpsInput(1);
        XTest obj = new XTest(1);
        try {
            queue.add(obj, 6_000L);
        } catch (Exception e) {
            Assertions.fail();
        }
        try {
            queue.add(obj, 6_000L);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(true);
        }

        queue.reset();
    }

    static class XTest {
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