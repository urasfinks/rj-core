package ru.jamsys.core.extension;

import lombok.Getter;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ProcedureThrowing;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class LifeCycleInterfaceSeqImpl {

    @Getter
    public static class ResultOperation{
        private final boolean complete;
        private final String cause;

        public ResultOperation(boolean complete, String cause) {
            this.complete = complete;
            this.cause = cause;
        }
    }

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private final AtomicBoolean run = new AtomicBoolean(false);

    // Запускать можем только полностью остановленное
    public ResultOperation run(ProcedureThrowing procedure) {
        if (isRun()) {
            return new ResultOperation(false, "Already run");
        }
        if (shuttingDown.get()) {
            return new ResultOperation(false, "Cannot be called while shuttingDown");
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
            return new ResultOperation(false, "Already running");
        }
        return new ResultOperation(true, "Ok");
    }

    // Завершаем только запущенное
    public ResultOperation shutdown(ProcedureThrowing procedure) {
        if (!isRun()) {
            return new ResultOperation(false, "Not run");
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
            return new ResultOperation(false, "Already shuttingDown");
        }
        return new ResultOperation(true, "Ok");
    }

    public boolean isRun() {
        return run.get();
    }

}