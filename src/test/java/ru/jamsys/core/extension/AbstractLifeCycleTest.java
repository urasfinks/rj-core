package ru.jamsys.core.extension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AbstractLifeCycleTest {

    private LifeCycleInterface lifeCycle;
    private AbstractLifeCycle impl;

    @BeforeEach
    void setUp() {
        impl = Mockito.spy(new AbstractLifeCycle() {
            @Override
            public void runOperation() {
            }

            @Override
            public void shutdownOperation() {
            }
        });
        lifeCycle = impl;
    }

    // ==================== Тесты для runSequential() ====================

    @Test
    void runSequential_successWhenNotRunning() {
        // Проверяем успешный запуск, если не был запущен
        LifeCycleInterface.ResultOperation result = lifeCycle.run();

        assertTrue(result.isComplete());
        assertEquals(LifeCycleInterface.Cause.SUCCESS, result.getCause());
        assertTrue(lifeCycle.getRun().get());
    }

    @Test
    void runSequential_failWhenAlreadyRunning() {
        // Устанавливаем состояние "уже запущен"
        lifeCycle.getRun().set(true);

        LifeCycleInterface.ResultOperation result = lifeCycle.run();

        assertFalse(result.isComplete());
        assertEquals(LifeCycleInterface.Cause.ALREADY_RUN, result.getCause());
    }

    @Test
    void runSequential_failWhenOperationInProgress() {
        // Захватываем мьютекс вручную
        lifeCycle.getOperation().set(true);

        LifeCycleInterface.ResultOperation result = lifeCycle.run();

        assertFalse(result.isComplete());
        assertEquals(LifeCycleInterface.Cause.OTHER_OPERATION_START, result.getCause());
    }

    // ==================== Тесты для shutdownSequential() ====================

    @Test
    void shutdownSequential_successWhenRunning() {
        // Устанавливаем состояние "запущен"
        lifeCycle.getRun().set(true);

        LifeCycleInterface.ResultOperation result = lifeCycle.shutdown();

        assertTrue(result.isComplete());
        assertEquals(LifeCycleInterface.Cause.SUCCESS, result.getCause());
        assertFalse(lifeCycle.getRun().get());
    }

    @Test
    void shutdownSequential_failWhenNotRunning() {
        LifeCycleInterface.ResultOperation result = lifeCycle.shutdown();

        assertFalse(result.isComplete());
        assertEquals(LifeCycleInterface.Cause.NOT_RUN, result.getCause());
    }

    @Test
    void shutdownSequential_failWhenOperationInProgress() {
        // Захватываем мьютекс вручную
        lifeCycle.getOperation().set(true);

        LifeCycleInterface.ResultOperation result = lifeCycle.shutdown();

        assertFalse(result.isComplete());
        assertEquals(LifeCycleInterface.Cause.OTHER_OPERATION_START, result.getCause());
    }

    // ==================== Тесты для reload() ====================

    @Test
    void reload_successWhenRunning() {
        // Устанавливаем состояние "запущен"
        lifeCycle.getRun().set(true);

        LifeCycleInterface.ResultOperation result = lifeCycle.reload();

        assertTrue(result.isComplete());
        assertEquals(LifeCycleInterface.Cause.SUCCESS, result.getCause());
        assertTrue(lifeCycle.getRun().get()); // После reload() состояние должно быть "запущен"
    }

    @Test
    void reload_successWhenNotRunning() {
        LifeCycleInterface.ResultOperation result = lifeCycle.reload();

        assertTrue(result.isComplete());
        assertEquals(LifeCycleInterface.Cause.SUCCESS, result.getCause());
        assertTrue(lifeCycle.getRun().get()); // После reload() состояние должно быть "запущен"
    }

    @Test
    void reload_failWhenOperationInProgress() {
        // Захватываем мьютекс вручную
        lifeCycle.getOperation().set(true);

        LifeCycleInterface.ResultOperation result = lifeCycle.reload();

        assertFalse(result.isComplete());
        assertEquals(LifeCycleInterface.Cause.OTHER_OPERATION_START, result.getCause());
    }

    // ==================== Проверка вызовов run() и shutdown() ====================

    @Test
    void runSequential_callsRunMethod() {
        lifeCycle.run();
        verify(impl, times(1)).runOperation();
    }

    @Test
    void shutdownSequential_callsShutdownMethod() {
        lifeCycle.getRun().set(true);
        lifeCycle.shutdown();
        verify(impl, times(1)).shutdownOperation();
    }

    @Test
    void reload_callsShutdownAndRun() {
        lifeCycle.getRun().set(true);
        lifeCycle.reload();
        verify(impl, times(1)).shutdownOperation();
        verify(impl, times(1)).runOperation();
    }

    @Test
    void testConcurrent() throws InterruptedException {
        AbstractLifeCycle lifeCycleInterfaceSeq = new AbstractLifeCycle() {
            @Override
            public void runOperation() {
                Util.testSleepMs(1000);
            }

            @Override
            public void shutdownOperation() {
                Util.testSleepMs(1000);
            }
        };
        Thread thread1 = new Thread(() -> {
            LifeCycleInterface.ResultOperation resultOperation = lifeCycleInterfaceSeq.run();
            assertTrue(resultOperation.isComplete());
        });
        thread1.start();
        thread1.join();

        Thread thread = new Thread(() -> {
            LifeCycleInterface.ResultOperation resultOperation1 = lifeCycleInterfaceSeq.shutdown();
            assertFalse(resultOperation1.isComplete());
            assertEquals(LifeCycleInterface.Cause.OTHER_OPERATION_START, resultOperation1.getCause());
        });
        thread.start();
        thread.join();

        Util.testSleepMs(1100);

        LifeCycleInterface.ResultOperation reload = lifeCycleInterfaceSeq.reload();
        assertTrue(reload.isComplete());

    }

    @Test
    void testConcurrentInterrupt() throws InterruptedException {


        AbstractLifeCycle lifeCycleInterfaceSeq = new AbstractLifeCycle() {

            final AtomicBoolean firstRun = new AtomicBoolean(true);

            @Override
            public void runOperation() {

            }

            @Override
            public void shutdownOperation() {
                // Первый запуск мы делаем задержку в 10 сек, и делам forceReload, что бы прекратить это ожидание
                // А второй
                if (firstRun.compareAndSet(true, false)) {
                    try {
                        Thread.sleep(10000);
                    } catch (Throwable th) {
                        throw new ForwardException(th);
                    }
                }
            }
        };

        Thread thread1 = new Thread(() -> {
            lifeCycleInterfaceSeq.run();
            assertThrows(ForwardException.class, lifeCycleInterfaceSeq::shutdown);
        });
        thread1.start();
        Util.testSleepMs(100);
        long start = System.currentTimeMillis();
        Thread thread2 = new Thread(() -> {
            LifeCycleInterface.ResultOperation reload = lifeCycleInterfaceSeq.forceReload();
            //UtilLog.printInfo(LifeCycleInterfaceSeqImplTest.class, reload);
            assertTrue(reload.isComplete());
        });
        thread2.start();

        thread1.join();
        thread2.join();

        assertTrue((System.currentTimeMillis() - start) < 2000);

    }

    @Test
    void runSequential_fromReloadDoesNotLockMutex() {
        lifeCycle.getRun().set(false);
        LifeCycleInterface.ResultOperation result = lifeCycle.run(true);
        assertTrue(result.isComplete());
        assertFalse(lifeCycle.getOperation().get()); // Мьютекс не должен быть захвачен
    }

    @Test
    void forceReload_whenNoThreadOperation() {
        LifeCycleInterface.ResultOperation result = lifeCycle.forceReload();
        assertTrue(result.isComplete()); // Должен работать как обычный reload
    }

    @Test
    void isRun_returnsCorrectValue() {
        assertFalse(lifeCycle.isRun());
        lifeCycle.getRun().set(true);
        assertTrue(lifeCycle.isRun());
    }

    @Test
    void threadOperationMethods_workCorrectly() {
        Thread thread = Thread.currentThread();
        lifeCycle.setThreadOperation(thread);
        assertEquals(thread, lifeCycle.getThreadOperation());
        lifeCycle.setThreadOperation(null);
        assertNull(lifeCycle.getThreadOperation());
    }

    @Test
    void reload_whenShutdownThrowsException() {
        LifeCycleInterface failingImpl = new AbstractLifeCycle() {
            @Override
            public void runOperation() {}
            @Override
            public void shutdownOperation() { throw new RuntimeException("Shutdown failed"); }
        };
        LifeCycleInterface.ResultOperation resultOperation = failingImpl.run();
        assertTrue(resultOperation.isComplete());
        assertThrows(RuntimeException.class, failingImpl::reload);
    }

}