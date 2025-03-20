package ru.jamsys.core.extension.raw.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbstractHotSwapTest {
    private AbstractHotSwap<Completed<String>, String> hotSwap;
    private Completed<String> mockResource;
    private Completed<String> mockNewResource;

    @BeforeEach
    void setUp() {
        // Создаем mock-объекты
        mockResource = mock(Completed.class);
        mockNewResource = mock(Completed.class);

        // Создаем анонимный класс для тестирования AbstractHotSwap
        hotSwap = new AbstractHotSwap<Completed<String>, String>() {
            @Override
            public Completed<String> getNextSwap(int sequence) {
                return mockNewResource;
            }
        };

        // Заменяем primary на mockResource для тестов
        hotSwap.primary = mockResource;
    }

    @Test
    void testInitialization() {
        assertNotNull(hotSwap.primary);
    }

    @Test
    void testSwapWhenPrimaryIsCompleted() {
        // Настраиваем mockResource как завершенный
        when(mockResource.isCompleted()).thenReturn(true);

        // Вызываем swap
        hotSwap.swap();

        // Проверяем, что primary был заменен на новый ресурс
        assertEquals(mockNewResource, hotSwap.primary);
        verify(mockResource, times(1)).release();
    }

    @Test
    void testSwapWhenPrimaryIsNotCompleted() {
        // Настраиваем mockResource как не завершенный
        when(mockResource.isCompleted()).thenReturn(false);

        // Вызываем swap
        hotSwap.swap();

        // Проверяем, что primary не был заменен
        assertEquals(mockResource, hotSwap.primary);
        verify(mockResource, never()).release();
    }

    @Test
    void testGetResourceWhenPrimaryIsCompleted() {
        // Настраиваем mockResource как завершенный
        when(mockResource.isCompleted()).thenReturn(true);
        when(mockNewResource.isCompleted()).thenReturn(false);
        when(mockNewResource.getIfNotCompleted()).thenReturn("NewResource");

        // Вызываем getResource
        String result = hotSwap.getResource();

        // Проверяем, что был возвращен новый ресурс
        assertEquals("NewResource", result);
        verify(mockResource, times(1)).release();
    }

    @Test
    void testGetResourceWhenPrimaryIsNotCompleted() {
        // Настраиваем mockResource как не завершенный
        when(mockResource.isCompleted()).thenReturn(false);
        when(mockResource.getIfNotCompleted()).thenReturn("Resource");

        // Вызываем getResource
        String result = hotSwap.getResource();

        // Проверяем, что был возвращен текущий ресурс
        assertEquals("Resource", result);
        verify(mockResource, never()).release();
    }

    @Test
    void testSwapRateLimit() {
        // Настраиваем mockResource как завершенный
        when(mockResource.isCompleted()).thenReturn(true);

        // Первый вызов swap
        hotSwap.swap();
        assertEquals(mockNewResource, hotSwap.primary);

        // Второй вызов swap (должен быть проигнорирован из-за ограничения времени)
        hotSwap.swap();
        assertEquals(mockNewResource, hotSwap.primary); // primary не должен измениться
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        // Настраиваем mockResource как завершенный
        when(mockResource.isCompleted()).thenReturn(true);
        when(mockNewResource.isCompleted()).thenReturn(false);
        when(mockNewResource.getIfNotCompleted()).thenReturn("NewResource");

        // Создаем несколько потоков для тестирования многопоточного доступа
        Runnable task = () -> {
            for (int i = 0; i < 10; i++) {
                String resource = hotSwap.getResource();
                assertNotNull(resource);
            }
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Проверяем, что ресурс был заменен только один раз
        verify(mockResource, times(1)).release();
    }
}