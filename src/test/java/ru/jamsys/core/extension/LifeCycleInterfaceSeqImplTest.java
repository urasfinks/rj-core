package ru.jamsys.core.extension;

import org.junit.jupiter.api.*;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ProcedureThrowing;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class LifeCycleInterfaceSeqImplTest {
    private LifeCycleInterfaceSeqImpl lifeCycle;
    private final ProcedureThrowing emptyProcedure = () -> {
    };

    @BeforeEach
    void setUp() {
        lifeCycle = new LifeCycleInterfaceSeqImpl();
    }

    @AfterEach
    void tearDown() {
        // Ensure we don't leave the object in a locked state
        if (lifeCycle.isRun()) {
            assertTrue(lifeCycle.shutdown(emptyProcedure).isComplete());
        }
    }

    @Test
    void testRunBasicFunctionality() {
        AtomicBoolean executed = new AtomicBoolean(false);
        ProcedureThrowing procedure = () -> executed.set(true);

        assertTrue(lifeCycle.run(procedure).isComplete());
        assertTrue(executed.get());
        assertTrue(lifeCycle.isRun());
    }

    @Test
    void testRunSetsRunningFlag() {
        ProcedureThrowing procedure = () -> assertFalse(lifeCycle.isRun());

        assertTrue(lifeCycle.run(procedure).isComplete());

        assertTrue(lifeCycle.isRun());
    }

    @Test
    @Timeout(1)
    void testRunWaitsForShutdownToComplete() throws InterruptedException {
        // Simulate shutdown in progress in another thread
        new Thread(() -> assertFalse(lifeCycle.shutdown(emptyProcedure).isComplete())).start();

        // Give the shutdown thread time to start
        Thread.sleep(50);

        AtomicBoolean executed = new AtomicBoolean(false);
        ProcedureThrowing procedure = () -> executed.set(true);

        assertTrue(lifeCycle.run(procedure).isComplete());

        assertTrue(executed.get());
    }

    @Test
    void testRunThrowsOnTimeout() {
        // Simulate long shutdown in another thread
        assertTrue(lifeCycle.run(() -> {}).isComplete());

        assertFalse(lifeCycle.getRunning().get());
        assertFalse(lifeCycle.getShuttingDown().get());
        assertTrue(lifeCycle.isRun());

        new Thread(() -> {
            try {
                assertTrue(lifeCycle.shutdown(() -> Thread.sleep(1000)).isComplete());
            } catch (Exception e) {
                // ignore
            }
        }).start();

        // Give the shutdown thread time to start
        try {
            Thread.sleep(50);
        } catch (InterruptedException _) {
        }

        assertFalse(lifeCycle.getRunning().get());
        assertTrue(lifeCycle.getShuttingDown().get());
        assertTrue(lifeCycle.isRun());

        assertFalse(lifeCycle.run(emptyProcedure).isComplete());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException _) {
        }
        assertTrue(lifeCycle.run(emptyProcedure).isComplete());
    }

    @Test
    void testShutdownBasicFunctionality() {
        AtomicBoolean executed = new AtomicBoolean(false);
        ProcedureThrowing procedure = () -> executed.set(true);

        assertTrue(lifeCycle.run(emptyProcedure).isComplete());
        assertTrue(lifeCycle.shutdown(procedure).isComplete());
        assertTrue(executed.get());
        assertFalse(lifeCycle.isRun());
    }

    @Test
    void testShutdownWaitsForRunToComplete() throws InterruptedException {
        AtomicBoolean shutdownExecuted = new AtomicBoolean(false);
        ProcedureThrowing shutdownProcedure = () -> shutdownExecuted.set(true);

        // Start run in another thread that will take some time
        new Thread(() -> {
            try {
                assertTrue(lifeCycle.run(() -> Thread.sleep(200)).isComplete());
            } catch (Exception e) {
                // ignore
            }
        }).start();

        // Give the run thread time to start
        Thread.sleep(50);
        assertFalse(lifeCycle.shutdown(shutdownProcedure).isComplete());

        // Так как run ещё не прошёл - останавливать нельзя
        assertFalse(shutdownExecuted.get());
    }

    @Test
    void testShutdownThrowsOnTimeout() {
        // Start long-running operation in another thread
        new Thread(() -> {
            try {
                assertTrue(lifeCycle.run(() -> Thread.sleep(1000)).isComplete());
            } catch (Exception e) {
                // ignore
            }
        }).start();

        // Give the run thread time to start
        try {
            Thread.sleep(50);
        } catch (InterruptedException _) {
        }
        assertFalse(lifeCycle.shutdown(emptyProcedure).isComplete());
    }

    @Test
    void testShutdownWithoutRun() {
        assertFalse(lifeCycle.shutdown(emptyProcedure).isComplete());
        // Should not throw, just log error
        assertFalse(lifeCycle.isRun());
    }

    @Test
    void testInterruptedExceptionHandling() {
        ProcedureThrowing interruptingProcedure = () -> Thread.currentThread().interrupt();

        assertTrue(lifeCycle.run(interruptingProcedure).isComplete());
        assertTrue(Thread.interrupted()); // clear the interrupt flag

        assertFalse(lifeCycle.run(emptyProcedure).isComplete());
        assertTrue(lifeCycle.shutdown(interruptingProcedure).isComplete());
        assertTrue(Thread.interrupted()); // clear the interrupt flag
    }

    @Test
    void testExceptionForwarding() {
        RuntimeException testException = new RuntimeException("Test exception");
        ProcedureThrowing throwingProcedure = () -> {
            throw testException;
        };

        ForwardException runException = assertThrows(ForwardException.class,
                () -> lifeCycle.run(throwingProcedure));
        assertEquals(testException, runException.getCause());

        //lifeCycle.run(emptyProcedure);
        ForwardException shutdownException = assertThrows(ForwardException.class,
                () -> lifeCycle.shutdown(throwingProcedure));
        assertEquals(testException, shutdownException.getCause());
    }
}