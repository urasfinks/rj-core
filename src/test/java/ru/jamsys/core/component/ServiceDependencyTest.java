package ru.jamsys.core.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceDependencyTest {

    private ServiceDependency dependency;

    static class ServiceA {}
    static class ServiceB {}
    static class ServiceC {}

    @BeforeEach
    void setUp() {
        dependency = new ServiceDependency();
    }

    @Test
    void testServiceIsActiveByDefault() {
        var status = dependency.get(ServiceA.class);
        var result = status.checkDependency();

        assertTrue(result.isActive());
        assertEquals("", result.getCause());
        assertEquals(List.of(), result.getDependencyChain());
    }

    @Test
    void testDeactivateAndCheck() {
        var status = dependency.get(ServiceA.class);
        status.deactivate("Ошибка сети");

        var result = status.checkDependency();

        assertFalse(result.isActive());
        assertTrue(result.getCause().contains("Ошибка сети"));
        assertEquals(List.of("ServiceA"), result.getDependencyChain());
    }

    @Test
    void testDependencyFailurePropagates() {
        var a = dependency.get(ServiceA.class);
        var b = dependency.get(ServiceB.class);

        b.addDependency(a);
        a.deactivate("Недоступен");

        var result = b.checkDependency();

        assertFalse(result.isActive());
        assertTrue(result.getCause().contains("ServiceA"));
        assertTrue(result.getDependencyChain().contains("ServiceA"));
        assertTrue(result.getDependencyChain().contains("ServiceB"));
    }

    @Test
    void testResetAllActivatesServices() {
        var a = dependency.get(ServiceA.class);
        var b = dependency.get(ServiceB.class);

        a.deactivate("Проблема A");
        b.deactivate("Проблема B");

        dependency.resetAll();

        assertTrue(a.checkDependency().isActive());
        assertTrue(b.checkDependency().isActive());
    }

    @Test
    void testCycleDetection() {
        var a = dependency.get(ServiceA.class);
        var b = dependency.get(ServiceB.class);
        var c = dependency.get(ServiceC.class);

        a.addDependency(b);
        b.addDependency(c);
        c.addDependency(a); // цикл

        var result = a.checkDependency();
        System.out.printf(result.getCause());
        assertFalse(result.isActive());
        assertTrue(result.getCause().contains("Циклическая зависимость"));
        assertTrue(result.getDependencyChain().contains("ServiceA"));
        assertTrue(result.getDependencyChain().contains("ServiceB"));
        assertTrue(result.getDependencyChain().contains("ServiceC"));
    }
}
