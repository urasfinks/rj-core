package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.component.manager.ManagerConfigurationFactory;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.extension.broker.BrokerRepositoryProperty;
import ru.jamsys.core.extension.broker.memory.BrokerMemory;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// IO time: 2.481
// COMPUTE time: 2.457

class BrokerMemoryTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void testLiner() {
        List<XTest> dropped = new ArrayList<>();
        ManagerConfiguration<BrokerMemory<XTest>> brokerMemoryManagerConfiguration = ManagerConfigurationFactory.get(
                BrokerMemory.class,
                XTest.class.getSimpleName() + "_1",
                managerElement -> {
                    managerElement.setOnPostDrop(dropped::add);
                }
        );

        BrokerMemory<XTest> broker = brokerMemoryManagerConfiguration.get();

        App.get(ServiceProperty.class)
                .computeIfAbsent(broker
                                .getPropertyDispatcher()
                                .getRepositoryProperty()
                                .getByFieldNameConstants(BrokerRepositoryProperty.Fields.size)
                                .getPropertyKey()
                        , null)
                .set(10);
        App.get(ServiceProperty.class)
                .computeIfAbsent(broker
                                .getPropertyDispatcher()
                                .getRepositoryProperty()
                                .getByFieldNameConstants(BrokerRepositoryProperty.Fields.tailSize)
                                .getPropertyKey(),
                        null)
                .set(3);

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
        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}]", dropped.toString(), "#8");

        List<XTest> cloneQueue = broker.getCloneQueue(null);
        Assertions.assertEquals("[XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}, XTest{x=14}]", cloneQueue.toString(), "#9");
        broker.reset();
        Assertions.assertEquals("[]", broker.getCloneQueue(null).toString(), "#10");
    }

    @Test
    void testCyclic() {
        ManagerConfiguration<BrokerMemory<XTest>> brokerMemoryManagerConfiguration = ManagerConfigurationFactory.get(
                BrokerMemory.class,
                XTest.class.getSimpleName()
        );
        BrokerMemory<XTest> broker = brokerMemoryManagerConfiguration.get();

        App.get(ServiceProperty.class)
                .computeIfAbsent(broker
                        .getPropertyDispatcher()
                        .getRepositoryProperty()
                        .getByFieldNameConstants(BrokerRepositoryProperty.Fields.size)
                        .getPropertyKey(), null)
                .set(10);
        App.get(ServiceProperty.class)
                .computeIfAbsent(broker
                        .getPropertyDispatcher()
                        .getRepositoryProperty()
                        .getByFieldNameConstants(BrokerRepositoryProperty.Fields.tailSize)
                        .getPropertyKey(), null)
                .set(3);

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
        ManagerConfiguration<BrokerMemory<XTest>> brokerMemoryManagerConfiguration = ManagerConfigurationFactory.get(
                BrokerMemory.class,
                XTest.class.getSimpleName()
        );
        BrokerMemory<XTest> broker = brokerMemoryManagerConfiguration.get();
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

        ManagerConfiguration<BrokerMemory<XTest>> brokerMemoryManagerConfiguration = ManagerConfigurationFactory.get(
                BrokerMemory.class,
                XTest.class.getSimpleName()
        );
        BrokerMemory<XTest> broker = brokerMemoryManagerConfiguration.get();
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
        ManagerConfiguration<BrokerMemory<XTest>> brokerMemoryManagerConfiguration = ManagerConfigurationFactory.get(
                BrokerMemory.class,
                XTest.class.getSimpleName()
        );
        BrokerMemory<XTest> broker = brokerMemoryManagerConfiguration.get();
        App.get(ServiceProperty.class)
                .computeIfAbsent(broker
                        .getPropertyDispatcher()
                        .getRepositoryProperty()
                        .getByFieldNameConstants(BrokerRepositoryProperty.Fields.size)
                        .getPropertyKey(), null)
                .set(3000);

        Assertions.assertEquals(3000, broker
                .getPropertyDispatcher()
                .getRepositoryProperty()
                .getByFieldNameConstants(BrokerRepositoryProperty.Fields.size)
                .getValue()
        );

        App.get(ServiceProperty.class)
                .computeIfAbsent(broker
                        .getPropertyDispatcher()
                        .getRepositoryProperty()
                        .getByFieldNameConstants(BrokerRepositoryProperty.Fields.size)
                        .getPropertyKey(), null)
                .set(3001);

        Assertions.assertEquals(3001, broker
                .getPropertyDispatcher()
                .getRepositoryProperty()
                .getByFieldNameConstants(BrokerRepositoryProperty.Fields.size)
                .getValue());
    }

    @Test
    void testSpeedRemove() {
        int selection = 1_000_000;

        ManagerConfiguration<BrokerMemory<XTest>> brokerMemoryManagerConfiguration = ManagerConfigurationFactory.get(
                BrokerMemory.class,
                XTest.class.getSimpleName()
        );
        BrokerMemory<XTest> broker = brokerMemoryManagerConfiguration.get();
        App.get(ServiceProperty.class)
                .computeIfAbsent(broker
                        .getPropertyDispatcher()
                        .getRepositoryProperty()
                        .getByFieldNameConstants(BrokerRepositoryProperty.Fields.size)
                        .getPropertyKey(), null)
                .set(3_000_000);

        List<DisposableExpirationMsImmutableEnvelope<XTest>> list = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < selection; i++) {
            XTest obj = new XTest(i);
            DisposableExpirationMsImmutableEnvelope<XTest> add = broker.add(obj, 4_000L);
            list.add(add);
        }
        long timeAdd = System.currentTimeMillis() - start;
        UtilLog.printAction("add time: " + timeAdd);
        Assertions.assertTrue(timeAdd < 600, "#3");
        start = System.currentTimeMillis();
        for (int i = 0; i < selection; i++) {
            broker.remove(list.get(selection - i - 1));
        }
        long timeRem = System.currentTimeMillis() - start;
        UtilLog.printAction("remove time: " + timeRem);
        Assertions.assertTrue(timeRem < 500, "#3");
        Assertions.assertEquals(0, broker.size(), "#3");
        start = System.currentTimeMillis();
        ExpirationMsImmutableEnvelope<XTest> xTestExpirationMsImmutableEnvelope = broker.pollLast();
        UtilLog.printAction("pool time: " + (System.currentTimeMillis() - start));
        Assertions.assertNull(xTestExpirationMsImmutableEnvelope, "#3");
    }

}