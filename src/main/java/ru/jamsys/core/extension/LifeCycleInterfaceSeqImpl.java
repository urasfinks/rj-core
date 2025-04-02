package ru.jamsys.core.extension;

import lombok.Getter;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ProcedureThrowing;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class LifeCycleInterfaceSeqImpl {

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private final AtomicBoolean run = new AtomicBoolean(false);

    // Запускать можем только полностью остановленное
    public void run(ProcedureThrowing procedure) {
        if (isRun()) {
            throw new RuntimeException("run() already run");
        }
        if (shuttingDown.get()) {
            throw new RuntimeException("run() cannot be called while shuttingDown");
        }
        if (running.compareAndSet(false, true)) {
            try {
                procedure.run();
            } catch (Throwable th) {
                throw new ForwardException(th);
            } finally {
                run.set(true);
                running.set(false);
            }
        } else {
            throw new RuntimeException("run() already running");
        }
    }

    // Завершаем только запущенное
    public void shutdown(ProcedureThrowing procedure) {
        if (!isRun()) {
            throw new RuntimeException("shutdown() cannot be called !isRun()");
        }
        if (shuttingDown.compareAndSet(false, true)) {
            try {
                procedure.run();
            } catch (Throwable th) {
                throw new ForwardException(th);
            } finally {
                run.set(false);
                shuttingDown.set(false);
            }
        } else {
            throw new RuntimeException("shutdown() already shuttingDown");
        }
    }

    public boolean isRun() {
        return run.get();
    }

}