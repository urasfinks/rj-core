package ru.jamsys.core.balancer;

import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BalancerWeightedRoundRobinTest {

    public static class TestServer implements WeightElement {
        @Getter
        private final String name;
        private final int weight;

        public TestServer(String name, int weight) {
            this.name = name;
            this.weight = weight;
        }

        @Override
        public int getWeight() {
            return weight;
        }

    }

    @Test
    public void testGetReturnsNullWhenEmpty() {
        var balancer = new BalancerWeightedRoundRobin<TestServer>();
        assertNull(balancer.get());
    }

    @Test
    public void testSingleElementAlwaysReturned() {
        var balancer = new BalancerWeightedRoundRobin<TestServer>();
        TestServer server = new TestServer("A", 10);
        balancer.add(server);

        for (int i = 0; i < 100; i++) {
            assertEquals(server, balancer.get());
        }
    }

    @Test
    public void testDistributionRoughlyRespectsWeights() {
        var balancer = new BalancerWeightedRoundRobin<TestServer>();

        TestServer a = new TestServer("A", 1);
        TestServer b = new TestServer("B", 3);

        balancer.add(a);
        balancer.add(b);

        Map<String, Integer> counts = new HashMap<>();
        counts.put("A", 0);
        counts.put("B", 0);

        int total = 10000;
        for (int i = 0; i < total; i++) {
            TestServer s = balancer.get();
            counts.put(s.getName(), counts.get(s.getName()) + 1);
        }

        double ratio = counts.get("B") / (double) counts.get("A");
        assertTrue(ratio > 2.5 && ratio < 3.5, "Ratio should be ~3:1, actual: " + ratio);
    }

    @Test
    public void testRebuildWithWeightChange() {
        var balancer = new BalancerWeightedRoundRobin<TestServer>();

        TestServer a = new TestServer("A", 5);
        balancer.add(a);

        TestServer newA = new TestServer("A", 0); // simulate weight drop
        balancer.remove(a);
        balancer.add(newA);

        balancer.rebuild();
        assertNull(balancer.get());
    }

    @Test
    public void testRemoveElement() {
        var balancer = new BalancerWeightedRoundRobin<TestServer>();

        TestServer a = new TestServer("A", 5);
        TestServer b = new TestServer("B", 5);

        balancer.add(a);
        balancer.add(b);

        balancer.remove(a);

        for (int i = 0; i < 100; i++) {
            assertEquals("B", balancer.get().getName());
        }
    }

}