package ru.jamsys.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.broker.BrokerQueue;

import java.util.List;

class BrokerTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.context = SpringApplication.run(App.class, args);
    }

    @Test
    void testLiner() throws Exception {
        Broker broker = App.context.getBean(Broker.class);
        BrokerQueue<XTest> b = broker.get(XTest.class);

        b.setCyclical(false);
        b.setSizeQueue(10);
        b.setSizeTail(3);

        for (int i = 0; i < 10; i++) {
            b.add(new XTest(i));
        }

        Assertions.assertEquals(10, b.getSize(), "#1");

        XTest t = b.pollFirst();
        Assertions.assertEquals(0, t.x, "#2");
        Assertions.assertEquals(9, b.getSize(), "#3");

        XTest t2 = b.pollLast();
        Assertions.assertEquals(9, t2.x, "#4");
        Assertions.assertEquals(8, b.getSize(), "#5");

        try {
            b.add(new XTest(11));
            b.add(new XTest(12));
            b.add(new XTest(13));
            b.add(new XTest(14));
            Assertions.assertFalse(true, "#6");
        } catch (Exception e) {
            Assertions.assertTrue(true, "#7");
        }
        List<XTest> tail = b.getTail();
        Assertions.assertEquals("[XTest{x=11}, XTest{x=12}, XTest{x=13}]", tail.toString(), "#8");
        Assertions.assertEquals("[XTest{x=1}, XTest{x=2}, XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}]", b.getCloneQueue().toString(), "#9");
        broker.shutdown();
        Assertions.assertEquals("[]", b.getCloneQueue().toString(), "#10");
    }

    @Test
    void testCyclic() throws Exception {
        Broker broker = App.context.getBean(Broker.class);
        BrokerQueue<XTest> b = broker.get(XTest.class);

        b.setCyclical(true);
        b.setSizeQueue(10);
        b.setSizeTail(3);

        for (int i = 0; i < 10; i++) {
            b.add(new XTest(i));
        }

        Assertions.assertEquals(10, b.getSize(), "#1");

        XTest t = b.pollFirst();
        Assertions.assertEquals(0, t.x, "#2");
        Assertions.assertEquals(9, b.getSize(), "#3");

        XTest t2 = b.pollLast();
        Assertions.assertEquals(9, t2.x, "#4");
        Assertions.assertEquals(8, b.getSize(), "#5");

        try {
            b.add(new XTest(11));
            b.add(new XTest(12));
            b.add(new XTest(13));
            b.add(new XTest(14));
            Assertions.assertTrue(true, "#6");
        } catch (Exception e) {
            Assertions.assertFalse(true, "#7");
        }
        List<XTest> tail = b.getTail();
        Assertions.assertEquals("[XTest{x=12}, XTest{x=13}, XTest{x=14}]", tail.toString(), "#8");
        Assertions.assertEquals("[XTest{x=3}, XTest{x=4}, XTest{x=5}, XTest{x=6}, XTest{x=7}, XTest{x=8}, XTest{x=11}, XTest{x=12}, XTest{x=13}, XTest{x=14}]", b.getCloneQueue().toString(), "#9");
        broker.shutdown();
        Assertions.assertEquals("[]", b.getCloneQueue().toString(), "#10");
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