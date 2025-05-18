package ru.jamsys.core.extension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class GraphTopologyTest {

    @Test
    void test() {
        GraphTopology<String> stringGraphTopology = new GraphTopology<>();
        stringGraphTopology.addDependency("Мать кошка", "котёнок");
        stringGraphTopology.addDependency("котёнок", "игрушка");

        Assertions.assertEquals("[игрушка, котёнок, Мать кошка]", stringGraphTopology.getSorted().toString());

        stringGraphTopology.add("перстень");
        stringGraphTopology.add("простыня");

        Assertions.assertEquals("[игрушка, котёнок, перстень, Мать кошка, простыня]", stringGraphTopology.getSorted().toString());

        stringGraphTopology.addDependency("простыня", "покрывало");

        Assertions.assertEquals("[игрушка, котёнок, покрывало, перстень, Мать кошка, простыня]", stringGraphTopology.getSorted().toString());

    }

    static class Service {
        final String name;

        Service(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Service)) return false;
            return name.equals(((Service) o).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    @Test
    void testSimpleDependencyOrder() {
        GraphTopology<Service> graph = new GraphTopology<>();
        Service a = new Service("A");
        Service b = new Service("B");
        Service c = new Service("C");

        graph.addDependency(a, b);
        graph.addDependency(b, c); // A -> B -> C

        List<Service> shutdownOrder = graph.getSorted();

        Assertions.assertEquals(List.of(c, b, a), shutdownOrder);
    }

    @Test
    void testIndependentNodes() {
        GraphTopology<Service> graph = new GraphTopology<>();
        Service a = new Service("A");
        Service b = new Service("B");

        graph.add(a);
        graph.add(b);

        List<Service> shutdownOrder = graph.getSorted();

        Assertions.assertTrue(shutdownOrder.contains(a));
        Assertions.assertTrue(shutdownOrder.contains(b));
        Assertions.assertEquals(2, shutdownOrder.size());
    }

    @Test
    void testRemoveDependency() {
        GraphTopology<Service> graph = new GraphTopology<>();
        Service a = new Service("A");
        Service b = new Service("B");

        graph.addDependency(a, b);
        graph.removeDependency(a, b);

        List<Service> shutdownOrder = graph.getSorted();

        // Порядок может быть любым, т.к. зависимости нет
        Assertions.assertTrue(shutdownOrder.contains(a));
        Assertions.assertTrue(shutdownOrder.contains(b));
    }

    @Test
    void testRemoveNode() {
        GraphTopology<Service> graph = new GraphTopology<>();
        Service a = new Service("A");
        Service b = new Service("B");
        Service c = new Service("C");

        graph.addDependency(a, b);
        graph.addDependency(b, c); // A -> B -> C

        graph.remove(b);

        List<Service> shutdownOrder = graph.getSorted();

        Assertions.assertEquals(2, shutdownOrder.size());
        Assertions.assertTrue(shutdownOrder.contains(a));
        Assertions.assertTrue(shutdownOrder.contains(c));
    }

    @Test
    void testCycleDetection() {
        GraphTopology<Service> graph = new GraphTopology<>();
        Service a = new Service("A");
        Service b = new Service("B");

        graph.addDependency(a, b);
        graph.addDependency(b, a); // цикл: A <-> B

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, graph::getSorted);
        Assertions.assertTrue(exception.getMessage().contains("Cycle detected"));
    }

}