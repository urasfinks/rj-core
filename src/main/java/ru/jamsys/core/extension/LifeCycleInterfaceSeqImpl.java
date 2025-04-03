package ru.jamsys.core.extension;

import lombok.Getter;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ProcedureThrowing;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class LifeCycleInterfaceSeqImpl {

    public enum Cause {
        ALREADY_RUN, // Уже запущен
        ALREADY_RUNNING, // В процессе запуска
        SHUTTING_DOWN, // В процессе остановки
        NOT_RUN, // Не запущен
        SUCCESS // Успех
    }

    @Getter
    public static class ResultOperation{
        private final boolean complete;
        private final Cause cause;

        public ResultOperation(boolean complete, Cause cause) {
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
            return new ResultOperation(false, Cause.ALREADY_RUN);
        }
        if (shuttingDown.get()) {
            return new ResultOperation(false, Cause.SHUTTING_DOWN);
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
            return new ResultOperation(false, Cause.ALREADY_RUNNING);
        }
        return new ResultOperation(true, Cause.SUCCESS);
    }

    // Завершаем только запущенное
    public ResultOperation shutdown(ProcedureThrowing procedure) {
        if (!isRun()) {
            return new ResultOperation(false, Cause.NOT_RUN);
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
            return new ResultOperation(false, Cause.SHUTTING_DOWN);
        }
        return new ResultOperation(true, Cause.SUCCESS);
    }

    public boolean isRun() {
        return run.get();
    }

}