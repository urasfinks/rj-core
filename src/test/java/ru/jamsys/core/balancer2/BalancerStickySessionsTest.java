package ru.jamsys.core.balancer2;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BalancerStickySessionsTest {
    private BalancerStickySessions<String> balancer;

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @BeforeEach
    void setUp() {
        // Переопределяем App.get(...) для тестов

        balancer = new BalancerStickySessions<>("test", 500); // 500ms TTL
        balancer.add("A");
        balancer.add("B");
        balancer.add("C");
    }

    @Test
    void returnsStickyElementForSameKey() {
        var result1 = balancer.get("client-1");
        var result2 = balancer.get("client-1");

        assertNotNull(result1);
        assertFalse(result1.value() == null);
        assertTrue(result1.isNew());

        assertEquals(result1.value(), result2.value());
        assertFalse(result2.isNew());
    }

    @Test
    void returnsDifferentElementForDifferentKey() {
        var result1 = balancer.get("client-1");
        var result2 = balancer.get("client-2");

        assertNotEquals(result1.value(), result2.value());
    }

    @Test
    void returnsNullWhenListIsEmpty() {
        var empty = new BalancerStickySessions<String>("empty", 1000);
        var result = empty.get("key");
        assertNull(result.value());
        assertFalse(result.isNew());
    }

    @Test
    void stickyEntryExpiresAfterInactivity() throws InterruptedException {
        // TODO: тест доделать сейчас не работает
//        var result1 = balancer.get("client-3");
//        assertTrue(result1.isNew());
//
//        // Ждём, пока entry истечёт
//        TimeUnit.MILLISECONDS.sleep(2000);
//
//        var result2 = balancer.get("client-3");
//        assertTrue(result2.isNew()); // Должно быть новым
//        assertNotEquals(result1.value(), result2.value()); // Может быть другим
    }

    @Test
    void removesStickyEntryWhenElementRemoved() {
        var result1 = balancer.get("client-4");
        String stickyValue = result1.value();

        balancer.remove(stickyValue);

        var result2 = balancer.get("client-4");
        assertTrue(result2.isNew());
        assertNotEquals(stickyValue, result2.value());
    }

}