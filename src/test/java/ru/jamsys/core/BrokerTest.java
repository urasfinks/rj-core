package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// IO time: 2.481
// COMPUTE time: 2.457

class BrokerTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
        App.get(ManagerBroker.class).initAndGet(XTest.class.getSimpleName(), XTest.class, _ -> System.out.println("DROP"));
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void testLiner() {
        List<XTest> droped = new ArrayList<>();
        Broker<XTest> broker = App.get(ManagerBroker.class)
                .initAndGet(XTest.class.getSimpleName() + "_1", XTest.class, xTest -> {
                    System.out.println("dropped: " + xTest);
                    droped.add(xTest);
                });
        broker.getPropertyBrokerSize().set(10);
        broker.getPropertyBrokerTailSize().set(3);

        for (int i = 0; i < 10; i++) {
            broker.add(new XTest(i), 6_000L);
        }
        Assertions.assertEquals("[XTest{x=0}, XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=9}]", broker.getCloneQueue(null).toString(), "#9");

        Assertions.assertEquals(10, broker.size(), "#1");

        ExpirationMsImmutableEnvelope<XTest> t = broker.pollFirst();
        Assertions.assertEquals(0, t.getValue().x, "#2");
        Assertions.assertEquals(9, broker.size(), "#3");

        ExpirationMsImmutableEnvelope<XTest> t2 = broker.pollLast();
        Assertions.assertEquals(9, t2.getValue().x, "#4");
        Assertions.assertEquals(8, broker.size(), "#5");

        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}]", broker.getCloneQueue(null).toString(), "#9");


        try {
            broker.add(new XTest(11), 6_000L);
            broker.add(new XTest(12), 6_000L);
            broker.add(new XTest(13), 6_000L);
            broker.add(new XTest(14), 6_000L);
        } catch (Exception e) {
            Assertions.assertTrue(true, "#7");
        }
        Assertions.assertEquals("[XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}, XTest{x=14}]", broker.getCloneQueue(null).toString(), "#9");

        List<XTest> tail = broker.getTailQueue(null);
        Assertions.assertEquals("[XTest{x=12}, XTest{x=13}, XTest{x=14}]", tail.toString(), "#8");
        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}]", droped.toString(), "#8");

        List<XTest> cloneQueue = broker.getCloneQueue(null);
        Assertions.assertEquals("[XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}, XTest{x=14}]", cloneQueue.toString(), "#9");
        broker.reset();
        Assertions.assertEquals("[]", broker.getCloneQueue(null).toString(), "#10");
    }

    @Test
    void testCyclic() {
        Broker<XTest> broker = App.get(ManagerBroker.class)
                .get(XTest.class.getSimpleName(), XTest.class);

        broker.getPropertyBrokerSize().set(10);
        broker.getPropertyBrokerTailSize().set(3);

        for (int i = 0; i < 10; i++) {
            broker.add(new XTest(i), 6_000L);
        }

        Assertions.assertEquals("[XTest{x=0}, XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=9}]", broker.getCloneQueue(null).toString());

        Assertions.assertEquals(10, broker.size());

        ExpirationMsImmutableEnvelope<XTest> t = broker.pollFirst();

        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=9}]", broker.getCloneQueue(null).toString());

        Assertions.assertEquals(0, t.getValue().x);
        Assertions.assertEquals(9, broker.size());

        ExpirationMsImmutableEnvelope<XTest> t2 = broker.pollLast();

        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}]", broker.getCloneQueue(null).toString());

        Assertions.assertEquals(9, t2.getValue().x);
        Assertions.assertEquals(8, broker.size());

        broker.add(new XTest(11), 6_000L);
        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}]", broker.getCloneQueue(null).toString());
        broker.add(new XTest(12), 6_000L);
        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}]", broker.getCloneQueue(null).toString());
        broker.add(new XTest(13), 6_000L);
        Assertions.assertEquals("[XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}]", broker.getCloneQueue(null).toString());
        broker.add(new XTest(14), 6_000L);
        Assertions.assertEquals("[XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}, XTest{x=14}]", broker.getCloneQueue(null).toString());
        Assertions.assertTrue(true, "#6");

        List<XTest> tail = broker.getTailQueue(null);
        Assertions.assertEquals("[XTest{x=12}, XTest{x=13}, XTest{x=14}]", tail.toString(), "#8");

        Assertions.assertEquals("[XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}, XTest{x=14}]", broker.getCloneQueue(null).toString());
        broker.reset();
        Assertions.assertEquals("[]", broker.getCloneQueue(null).toString(), "#10");

    }

    @Test
    void testReference() {
        Broker<XTest> broker = App.get(ManagerBroker.class)
                .get(XTest.class.getSimpleName(), XTest.class);
        XTest obj = new XTest(1);
        DisposableExpirationMsImmutableEnvelope<XTest> o1 = broker.add(obj, 6_000L);

        List<XTest> cloneQueue = broker.getCloneQueue(null);
        Assertions.assertEquals(obj.hashCode(), cloneQueue.getFirst().hashCode(), "#1");
        broker.remove(o1);
        Assertions.assertEquals(0, broker.size(), "#1");
        ExpirationMsImmutableEnvelope<XTest> xTestExpirationMsImmutableEnvelope = broker.pollLast();
        Assertions.assertNull(xTestExpirationMsImmutableEnvelope, "#2");
        Assertions.assertEquals(0, broker.size(), "#1");
        broker.reset();
    }

    @Test
    void testReference2() {
        AtomicBoolean run = new AtomicBoolean(true);

        Broker<XTest> broker = App.get(ManagerBroker.class)
                .get(XTest.class.getSimpleName(), XTest.class);
        XTest obj = new XTest(1);
        XTest obj2 = new XTest(2);
        DisposableExpirationMsImmutableEnvelope<XTest> o1 = null;
        try {
            o1 = broker.add(obj, 6_000L);
        } catch (Exception _) {

        }
        DisposableExpirationMsImmutableEnvelope<XTest> o2 = broker.add(obj2, 6_000L);

        List<XTest> cloneQueue = broker.getCloneQueue(run);
        Assertions.assertEquals(obj.hashCode(), cloneQueue.get(0).hashCode(), "#1");
        Assertions.assertEquals(obj2.hashCode(), cloneQueue.get(1).hashCode(), "#2");
        broker.remove(o1);
        Assertions.assertEquals(1, broker.size(), "#3");
        broker.remove(o2);
        Assertions.assertEquals(0, broker.size(), "#4");
        broker.reset();
    }

    @Test
    void testExpired() {
        AtomicInteger counter = new AtomicInteger(0);
        Broker<XTest> broker = App.get(ManagerBroker.class)
                .initAndGet(XTest.class.getSimpleName() + "_2", XTest.class, _ -> counter.incrementAndGet());

        XTest obj = new XTest(1);
        broker.add(obj, 1_000L);

        Assertions.assertEquals(0, counter.get());
        Util.sleepMs(2001);
        Assertions.assertEquals(1, counter.get());
    }

    static class XTest {
        final int x;

        XTest(int x) {
            this.x = x;
        }

        @SuppressWarnings("unused")
        void x() {
        }

        @Override
        public String toString() {
            return "XTest{" +
                    "x=" + x +
                    '}';
        }

    }

    @Test
    void testProperty() {
        App.get(ServiceProperty.class).setProperty("Broker.XTest.BrokerSize", "3000");
        Broker<XTest> broker = App.get(ManagerBroker.class).get(XTest.class.getSimpleName(), XTest.class);

        Assertions.assertEquals(3000, broker.getPropertyBrokerSize().get());
        broker.getPropertyBrokerSize().set(3001);
        Assertions.assertEquals(3001, broker.getPropertyBrokerSize().get());
    }

    @Test
    void testPropertyDo() {
        App.get(ServiceProperty.class).setProperty("Broker.XTest.BrokerSize", "11");
        Broker<XTest> broker = App.get(ManagerBroker.class).get(XTest.class.getSimpleName(), XTest.class);
        Assertions.assertEquals(11, broker.getPropertyBrokerSize().get());
    }

    @Test
    void testSpeedRemove() {
        int selection = 1_000_000;
        App.get(ServiceProperty.class).setProperty("Broker.XTest.BrokerSize", "3000000");
        Broker<XTest> broker = App.get(ManagerBroker.class).get(XTest.class.getSimpleName(), XTest.class);
        List<DisposableExpirationMsImmutableEnvelope<XTest>> list = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < selection; i++) {
            XTest obj = new XTest(i);
            DisposableExpirationMsImmutableEnvelope<XTest> add = broker.add(obj, 4_000L);
            list.add(add);
        }
        long timeAdd = System.currentTimeMillis() - start;
        System.out.println("add time: " + timeAdd);
        Assertions.assertTrue(timeAdd < 600, "#3");
        start = System.currentTimeMillis();
        for (int i = 0; i < selection; i++) {
            broker.remove(list.get(selection - i - 1));
        }
        long timeRem = System.currentTimeMillis() - start;
        System.out.println("remove time: " + timeRem);
        Assertions.assertTrue(timeRem < 500, "#3");
        Assertions.assertEquals(0, broker.size(), "#3");
        start = System.currentTimeMillis();
        ExpirationMsImmutableEnvelope<XTest> xTestExpirationMsImmutableEnvelope = broker.pollLast();
        System.out.println("pool time: " + (System.currentTimeMillis() - start));
        Assertions.assertNull(xTestExpirationMsImmutableEnvelope, "#3");
    }

}