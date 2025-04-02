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
    void tearDown() throws InterruptedException {
        // Ensure we don't leave the object in a locked state
        if (lifeCycle.isRun()) {
            lifeCycle.shutdown(emptyProcedure);
        }
    }

    @Test
    void testRunBasicFunctionality() {
        AtomicBoolean executed = new AtomicBoolean(false);
        ProcedureThrowing procedure = () -> executed.set(true);

        lifeCycle.run(procedure);

        assertTrue(executed.get());
        assertTrue(lifeCycle.isRun());
    }

    @Test
    void testRunSetsRunningFlag() {
        ProcedureThrowing procedure = () -> assertFalse(lifeCycle.isRun());

        lifeCycle.run(procedure);

        assertTrue(lifeCycle.isRun());
    }

    @Test
    @Timeout(1)
    void testRunWaitsForShutdownToComplete() throws InterruptedException {
        // Simulate shutdown in progress in another thread
        new Thread(() -> {
            lifeCycle.shutdown(emptyProcedure);
        }).start();

        // Give the shutdown thread time to start
        Thread.sleep(50);

        AtomicBoolean executed = new AtomicBoolean(false);
        ProcedureThrowing procedure = () -> executed.set(true);

        lifeCycle.run(procedure);

        assertTrue(executed.get());
    }

    @Test
    void testRunThrowsOnTimeout() {
        // Simulate long shutdown in another thread
        lifeCycle.run(() -> {

        });

        assertFalse(lifeCycle.getRunning().get());
        assertFalse(lifeCycle.getShuttingDown().get());
        assertTrue(lifeCycle.isRun());

        new Thread(() -> {
            try {
                lifeCycle.shutdown(() -> Thread.sleep(1000));
            } catch (Exception e) {
                // ignore
            }
        }).start();

        // Give the shutdown thread time to start
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }

        assertFalse(lifeCycle.getRunning().get());
        assertTrue(lifeCycle.getShuttingDown().get());
        assertTrue(lifeCycle.isRun());

        assertThrows(RuntimeException.class, () -> {
            lifeCycle.run(emptyProcedure);
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        lifeCycle.run(emptyProcedure);
    }

    @Test
    void testShutdownBasicFunctionality() {
        AtomicBoolean executed = new AtomicBoolean(false);
        ProcedureThrowing procedure = () -> executed.set(true);

        lifeCycle.run(emptyProcedure);
        lifeCycle.shutdown(procedure);

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
                lifeCycle.run(() -> Thread.sleep(200));
            } catch (Exception e) {
                // ignore
            }
        }).start();

        // Give the run thread time to start
        Thread.sleep(50);

        assertThrows(RuntimeException.class, () -> {
            lifeCycle.shutdown(shutdownProcedure);
        });

        // Так как run ещё не прошёл - останавливать нельзя
        assertFalse(shutdownExecuted.get());
    }

    @Test
    void testShutdownThrowsOnTimeout() {
        // Start long-running operation in another thread
        new Thread(() -> {
            try {
                lifeCycle.run(() -> Thread.sleep(1000));
            } catch (Exception e) {
                // ignore
            }
        }).start();

        // Give the run thread time to start
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }

        assertThrows(RuntimeException.class, () -> {
            lifeCycle.shutdown(emptyProcedure);
        });
    }

    @Test
    void testShutdownWithoutRun() {
        assertThrows(RuntimeException.class, () -> {
            lifeCycle.shutdown(emptyProcedure);
        });
        // Should not throw, just log error
        assertFalse(lifeCycle.isRun());
    }

    @Test
    void testInterruptedExceptionHandling() {
        ProcedureThrowing interruptingProcedure = () -> {
            Thread.currentThread().interrupt();
        };

        lifeCycle.run(interruptingProcedure);
        assertTrue(Thread.interrupted()); // clear the interrupt flag


        assertThrows(RuntimeException.class, () -> lifeCycle.run(emptyProcedure));
        lifeCycle.shutdown(interruptingProcedure);
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