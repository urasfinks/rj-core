package ru.jamsys.core.component;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class BrokerTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
        App.context.getBean(BrokerManager.class).initAndGet(XTest.class.getSimpleName(), XTest.class, null);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void testLiner() {
        Broker<XTest> broker = App.context.getBean(BrokerManager.class).get(XTest.class.getSimpleName(), XTest.class);
        broker.setMaxSizeQueue(10);
        broker.setMaxSizeQueueTail(3);

        for (int i = 0; i < 10; i++) {
            broker.add(new XTest(i), 6_000L);
        }
        List<XTest> droped = new ArrayList<>();
        broker.setOnDrop(droped::add);

        Assertions.assertEquals(10, broker.size(), "#1");

        ExpirationMsImmutableEnvelope<XTest> t = broker.pollFirst();
        Assertions.assertEquals(0, t.getValue().x, "#2");
        Assertions.assertEquals(9, broker.size(), "#3");

        ExpirationMsImmutableEnvelope<XTest> t2 = broker.pollLast();
        Assertions.assertEquals(9, t2.getValue().x, "#4");
        Assertions.assertEquals(8, broker.size(), "#5");

        try {
            broker.add(new XTest(11), 6_000L);
            broker.add(new XTest(12), 6_000L);
            broker.add(new XTest(13), 6_000L);
            broker.add(new XTest(14), 6_000L);
        } catch (Exception e) {
            Assertions.assertTrue(true, "#7");
        }
        List<XTest> tail = broker.getTail(null);
        Assertions.assertEquals("[XTest{x=12}, XTest{x=13}, XTest{x=14}]", tail.toString(), "#8");
        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}]", droped.toString(), "#8");

        List<XTest> cloneQueue = broker.getCloneQueue(null);
        Assertions.assertEquals("[XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}, XTest{x=14}]", cloneQueue.toString(), "#9");
        broker.reset();
        Assertions.assertEquals("[]", broker.getCloneQueue(null).toString(), "#10");
    }

    @Test
    void testCyclic() {
        Broker<XTest> broker = App.context.getBean(BrokerManager.class)
                .get(XTest.class.getSimpleName(), XTest.class);

        broker.setMaxSizeQueue(10);
        broker.setMaxSizeQueueTail(3);

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

        List<XTest> tail = broker.getTail(null);
        Assertions.assertEquals("[XTest{x=12}, XTest{x=13}, XTest{x=14}]", tail.toString(), "#8");

        Assertions.assertEquals("[XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}, XTest{x=14}]", broker.getCloneQueue(null).toString());
        broker.reset();
        Assertions.assertEquals("[]", broker.getCloneQueue(null).toString(), "#10");

    }

    @Test
    void testReference() {
        Broker<XTest> broker = App.context.getBean(BrokerManager.class)
                .get(XTest.class.getSimpleName(), XTest.class);
        XTest obj = new XTest(1);
        DisposableExpirationMsImmutableEnvelope<XTest> o1 = broker.add(obj, 6_000L);

        List<XTest> cloneQueue = broker.getCloneQueue(null);
        Assertions.assertEquals(obj.hashCode(), cloneQueue.getFirst().hashCode(), "#1");
        broker.remove(o1);
        Assertions.assertEquals(0, broker.size(), "#1");
        broker.reset();
    }

    @Test
    void testReference2() {
        AtomicBoolean isRun = new AtomicBoolean(true);

        Broker<XTest> broker = App.context.getBean(BrokerManager.class)
                .get(XTest.class.getSimpleName(), XTest.class);
        XTest obj = new XTest(1);
        XTest obj2 = new XTest(2);
        DisposableExpirationMsImmutableEnvelope<XTest> o1 = null;
        try {
            o1 = broker.add(obj, 6_000L);
        } catch (Exception _) {

        }
        DisposableExpirationMsImmutableEnvelope<XTest> o2 = broker.add(obj2, 6_000L);

        List<XTest> cloneQueue = broker.getCloneQueue(isRun);
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
        Broker<XTest> broker = App.context.getBean(BrokerManager.class)
                .get(XTest.class.getSimpleName(), XTest.class);
        AtomicInteger counter = new AtomicInteger(0);
        broker.setOnDrop(_ -> counter.incrementAndGet());
        XTest obj = new XTest(1);
        broker.add(obj, 1_000L);

        Assertions.assertEquals(0, counter.get());
        Util.sleepMs(1001);
        broker.keepAlive(null);
        broker.keepAlive(null);
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
        App.context.getBean(PropertyComponent.class).setProperty("RateLimit.Broker.XTest.BrokerSize.max", "3000");
        Broker<XTest> broker = App.context.getBean(BrokerManager.class).get(XTest.class.getSimpleName(), XTest.class);
        RateLimitItem rateLimitItem = broker.getRateLimit().get(RateLimitName.BROKER_SIZE.getName());
        Assertions.assertEquals(3000, rateLimitItem.get());
        rateLimitItem.set("max", 3001);
        Assertions.assertEquals(3001, rateLimitItem.get());
    }

    @Test
    void testPropertyDo() {
        App.context.getBean(PropertyComponent.class).setProperty("RateLimit.Broker.XTest.BrokerSize.max", "11");
        Broker<XTest> broker = App.context.getBean(BrokerManager.class).get(XTest.class.getSimpleName(), XTest.class);
        RateLimitItem rateLimitItem = broker.getRateLimit().get(RateLimitName.BROKER_SIZE.getName());
        Assertions.assertEquals(11, rateLimitItem.get());
    }

}