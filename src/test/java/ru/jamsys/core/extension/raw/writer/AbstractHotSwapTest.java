package ru.jamsys.core.extension.raw.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbstractHotSwapTest {


    private AbstractHotSwap<Completable> hotSwap;
    private Completable mockResource;
    private Completable mockNewResource;
    private Consumer<Completable> mockOnSwap;

    //---------------

    private AbstractHotSwap<TestCompletable> hotSwap2;
    private Consumer<TestCompletable> onSwapMock2;

    @BeforeEach
    void setUp() {
        // Создаем mock-объекты
        mockResource = mock(Completable.class);
        mockNewResource = mock(Completable.class);
        @SuppressWarnings("unchecked")
        Consumer<Completable> t1 = (Consumer<Completable>) mock(Consumer.class);
        mockOnSwap = t1;

        // Создаем анонимный класс для тестирования AbstractHotSwap
        hotSwap = new AbstractHotSwap<>() {
            @Override
            public Completable getNextHotSwap(int sequence) {
                return mockNewResource;
            }
        };

        // Заменяем resource на mockResource для тестов
        hotSwap.resource = mockResource;
        hotSwap.setOnSwap(mockOnSwap);
        //-------------------------------------

        @SuppressWarnings("unchecked")
        Consumer<TestCompletable> t2 = (Consumer<TestCompletable>) mock(Consumer.class);
        onSwapMock2 = t2;
        hotSwap2 = new AbstractHotSwap<>() {
            @Override
            public TestCompletable getNextHotSwap(int seq) {
                return new TestCompletable();
            }
        };
        hotSwap2.setOnSwap(onSwapMock2);
    }

    @Test
    void testInitialization() {
        assertNotNull(hotSwap2.getResource());
    }

    @Test
    void testSwapWhenResourceIsCompleted() {
        TestCompletable oldResource = hotSwap2.getResource();
        oldResource.complete();
        hotSwap2.swap();
        TestCompletable newResource = hotSwap2.getResource();

        assertNotSame(oldResource, newResource);
        verify(onSwapMock2).accept(oldResource);
    }

    @Test
    void testSwapWhenResourceIsNotCompleted() {
        TestCompletable oldResource = hotSwap2.getResource();
        hotSwap2.swap();
        TestCompletable newResource = hotSwap2.getResource();

        assertSame(oldResource, newResource);
        verifyNoInteractions(onSwapMock2);
    }

    @Test
    void testGetResourceWhenResourceIsCompleted() {
        TestCompletable oldResource = hotSwap2.getResource();
        oldResource.complete();
        TestCompletable newResource = hotSwap2.getResource();

        assertNotSame(oldResource, newResource);
        verify(onSwapMock2).accept(oldResource);
    }

    @Test
    void testGetResourceWhenResourceIsNotCompleted() {
        TestCompletable oldResource = hotSwap2.getResource();
        TestCompletable newResource = hotSwap2.getResource();

        assertSame(oldResource, newResource);
    }

    @Test
    void testSwapRateLimit() throws InterruptedException {
        TestCompletable first = hotSwap2.getResource();
        first.complete();
        hotSwap2.swap();
        TestCompletable second = hotSwap2.getResource();
        hotSwap2.swap(); // Должен игнорироваться, так как не прошло 1 секунды
        TestCompletable third = hotSwap2.getResource();

        assertSame(second, third);
        Thread.sleep(1001);
        second.complete();
        hotSwap2.swap();
        TestCompletable fourth = hotSwap2.getResource();

        assertNotSame(second, fourth);
    }

    @Test
    void testOnCloseCalled() {
        TestCompletable oldResource = hotSwap2.getResource();
        oldResource.complete();
        hotSwap2.swap();
        verify(onSwapMock2).accept(oldResource);
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<TestCompletable> initialResource = new AtomicReference<>(hotSwap2.getResource());

        Runnable task = () -> {
            hotSwap2.getResource();
            latch.countDown();
        };

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(task);
            threads[i].start();
        }

        latch.await();
        assertSame(initialResource.get(), hotSwap2.getResource());
    }

    static class TestCompletable implements ru.jamsys.core.extension.raw.writer.Completable {
        private volatile boolean completed = false;

        TestCompletable() {
        }

        void complete() {
            completed = true;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }
    }

    @Test
    void testInitialization0() {
        assertNotNull(hotSwap.resource);
    }

    @Test
    void testSwapWhenResourceIsCompleted0() {
        // Настраиваем mockResource как завершенный
        when(mockResource.isCompleted()).thenReturn(true);

        // Вызываем swap
        hotSwap.swap();

        // Проверяем, что resource был заменен на новый ресурс
        assertEquals(mockNewResource, hotSwap.resource);
    }

    @Test
    void testSwapWhenResourceIsNotCompleted0() {
        // Настраиваем mockResource как не завершенный
        when(mockResource.isCompleted()).thenReturn(false);

        // Вызываем swap
        hotSwap.swap();

        // Проверяем, что resource не был заменен
        assertEquals(mockResource, hotSwap.resource);
    }

    @Test
    void testGetResourceWhenResourceIsCompleted0() {
        // Настраиваем mockResource как завершенный
        when(mockResource.isCompleted()).thenReturn(true);

        // Вызываем getResource
        Completable result = hotSwap.getResource();

        // Проверяем, что был возвращен новый ресурс
        assertEquals(mockNewResource, result);
    }

    @Test
    void testGetResourceWhenResourceIsNotCompleted0() {
        // Настраиваем mockResource как не завершенный
        when(mockResource.isCompleted()).thenReturn(false);

        // Вызываем getResource
        Completable result = hotSwap.getResource();

        // Проверяем, что был возвращен текущий ресурс
        assertEquals(mockResource, result);
    }

    @Test
    void testSwapRateLimit0() {
        // Настраиваем mockResource как завершенный
        when(mockResource.isCompleted()).thenReturn(true);

        // Первый вызов swap
        hotSwap.swap();
        assertEquals(mockNewResource, hotSwap.resource);

        // Второй вызов swap (должен быть проигнорирован из-за ограничения времени)
        hotSwap.swap();
        assertEquals(mockNewResource, hotSwap.resource); // resource не должен измениться
    }

    @Test
    void testOnSwapCalled() {
        // Настраиваем mockResource как завершенный
        when(mockResource.isCompleted()).thenReturn(true);
        hotSwap.setOnSwap(mockOnSwap);

        // Вызываем swap
        hotSwap.swap();

        // Проверяем, что onSwap был вызван
        verify(mockOnSwap, times(1)).accept(mockResource);
    }

    @Test
    void testConcurrentAccess0() throws InterruptedException {
        // Настраиваем mockResource как завершенный
        when(mockResource.isCompleted()).thenReturn(true);

        // Создаем несколько потоков для тестирования многопоточного доступа
        Runnable task = () -> {
            for (int i = 0; i < 10; i++) {
                Completable resource = hotSwap.getResource();
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
        verify(mockOnSwap, times(1)).accept(mockResource);
    }

}