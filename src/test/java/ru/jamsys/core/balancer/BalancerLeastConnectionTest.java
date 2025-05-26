package ru.jamsys.core.balancer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BalancerLeastConnectionTest {

    public static class TestElement implements LeastConnectionElement {
        private final String name;
        private final int count;

        public TestElement(String name, int count) {
            this.name = name;
            this.count = count;
        }

        @Override
        public int getCountConnection() {
            return count;
        }

        @Override
        public String toString() {
            return name + "(" + count + ")";
        }
    }

    private BalancerLeastConnection<TestElement> balancer_1;

    @BeforeEach
    public void setup() {
        balancer_1 = new BalancerLeastConnection<>();

        balancer_2 = new BalancerLeastConnection<>();
        a = new DynamicTestElement("A", 5);
        b = new DynamicTestElement("B", 3);
        c = new DynamicTestElement("C", 7);
        balancer_2.add(a);
        balancer_2.add(b);
        balancer_2.add(c);
    }

    @Test
    public void testEmptyBalancerReturnsException() {
        assertThrows(NoSuchElementException.class, () -> balancer_1.get());
    }

    @Test
    public void testSingleElement() {
        TestElement elem = new TestElement("A", 3);
        balancer_1.add(elem);
        assertSame(elem, balancer_1.get());
    }

    @Test
    public void testMultipleElementsReturnsLeast() {
        TestElement a = new TestElement("A", 5);
        TestElement b = new TestElement("B", 2);
        TestElement c = new TestElement("C", 3);
        balancer_1.add(a);
        balancer_1.add(b);
        balancer_1.add(c);

        TestElement result = balancer_1.get();
        assertSame(b, result);
    }

    @Test
    public void testRemoveElement() {
        TestElement a = new TestElement("A", 1);
        TestElement b = new TestElement("B", 2);
        balancer_1.add(a);
        balancer_1.add(b);
        balancer_1.remove(a);

        assertSame(b, balancer_1.get());
    }

    @Test
    public void testAddNullIsIgnored() {
        balancer_1.add(null);
        assertTrue(balancer_1.getList().isEmpty());
    }

    @Test
    public void testRemoveNullIsIgnored() {
        balancer_1.remove(null); // Should not throw
    }

    public static class DynamicTestElement implements LeastConnectionElement {
        private final String name;
        private final AtomicInteger connectionCount = new AtomicInteger();

        public DynamicTestElement(String name, int initialCount) {
            this.name = name;
            this.connectionCount.set(initialCount);
        }

        public void setCount(int count) {
            this.connectionCount.set(count);
        }

        @Override
        public int getCountConnection() {
            return connectionCount.get();
        }

        @Override
        public String toString() {
            return name + "(" + connectionCount.get() + ")";
        }

    }

    private BalancerLeastConnection<DynamicTestElement> balancer_2;
    private DynamicTestElement a, b, c;

    @Test
    public void testInitialLeastConnection() {
        assertSame(b, balancer_2.get(), "Should initially return element with least connections (B)");
    }

    @Test
    public void testAfterDynamicChange() {
        // C становится самым лёгким
        c.setCount(1);
        assertSame(c, balancer_2.get(), "Should return C after it becomes least loaded");

        // A становится самым лёгким
        a.setCount(0);
        assertSame(a, balancer_2.get(), "Should return A after it becomes least loaded");
    }

    @Test
    public void testTieBreakerReturnsFirstMatch() {
        b.setCount(2);
        c.setCount(2);
        a.setCount(5);

        DynamicTestElement result = balancer_2.get();
        assertTrue(result == b || result == c, "Should return either B or C with same minimal load");
    }

    @Test
    public void testRemoveElementAffectsResult() {
        balancer_2.remove(b);
        assertSame(a, balancer_2.get(), "After removing B, A is next least loaded");
    }

    @Test
    public void testAllCountsEqual() {
        a.setCount(5);
        b.setCount(5);
        c.setCount(5);
        DynamicTestElement result = balancer_2.get();
        assertTrue(List.of(a, b, c).contains(result), "Should return any element when all counts are equal");
    }

}