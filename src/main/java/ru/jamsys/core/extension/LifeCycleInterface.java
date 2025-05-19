package ru.jamsys.core.extension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.ProcedureThrowing;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface LifeCycleInterface {

    List<ProcedureThrowing> getListOnPostShutdown();

    @JsonIgnore
    List<LifeCycleInterface> getListShutdownAfter();

    @JsonIgnore
    List<LifeCycleInterface> getListShutdownBefore();

    void runOperation(); // Обычный метод запуска, который требует реализацию, однако вызывать стоит runSequential

    void shutdownOperation(); // Обычный метод остановки, который требует реализацию, однако вызывать стоит shutdownSequential

    AtomicBoolean getOperation(); // Глобальный мьютекс для всех операций

    AtomicBoolean getRun(); // Статус на текущий момент

    void setThreadOperation(Thread thread);

    Thread getThreadOperation();

    enum Cause {
        ALREADY_RUN, // Уже запущен
        NOT_RUN, // Не запущен
        SUCCESS, // Успех
        OTHER_OPERATION_START
    }

    enum Process {
        RUN, //  Запуск
        SHUTDOWN, // Остановка
        RELOAD //Перезагрузка
    }

    @Getter
    @ToString
    class ResultOperation {
        private final Process process;
        private final boolean complete;
        private final Cause cause;


        public ResultOperation(Process process, boolean complete, Cause cause) {
            this.process = process;
            this.complete = complete;
            this.cause = cause;
        }
    }

    default ResultOperation run() {
        return run(false);
    }

    default ResultOperation run(boolean fromReload) {
        if (fromReload || getOperation().compareAndSet(false, true)) {
            setThreadOperation(Thread.currentThread());
            try {
                if (getRun().get()) {
                    return new ResultOperation(Process.RUN, false, Cause.ALREADY_RUN);
                }
                runOperation();
                getRun().set(true);
                return new ResultOperation(Process.RUN, true, Cause.SUCCESS);
            } finally {
                setThreadOperation(null);
                getOperation().set(false);
            }
        } else {
            return new ResultOperation(Process.RUN, false, Cause.OTHER_OPERATION_START);
        }
    }

    default ResultOperation shutdown() {
        return shutdown(false);
    }

    default ResultOperation shutdown(boolean fromReload) {
        if (fromReload || getOperation().compareAndSet(false, true)) {
            setThreadOperation(Thread.currentThread());
            try {
                if (!getRun().get()) {
                    return new ResultOperation(Process.SHUTDOWN, false, Cause.NOT_RUN);
                }
                shutdownOperation();
                getRun().set(false);
                getListOnPostShutdown().forEach(procedureThrowing -> {
                    try {
                        procedureThrowing.run();
                    } catch (Throwable th) {
                        App.error(th);
                    }
                });
                return new ResultOperation(Process.SHUTDOWN, true, Cause.SUCCESS);
            } finally {
                getOperation().set(false);
                setThreadOperation(null);
            }
        } else {
            return new ResultOperation(Process.SHUTDOWN, false, Cause.OTHER_OPERATION_START);
        }
    }

    default ResultOperation reload() {
        if (getOperation().compareAndSet(false, true)) {
            try {
                // Любой результат завершения нас устроит, так как в случае с fromReload = true может быть только
                // NOT_RUN и для перезагрузки это нормально
                shutdown(true);
                return run(true);
            } finally {
                getOperation().set(false);
            }
        } else {
            return new ResultOperation(Process.RELOAD, false, Cause.OTHER_OPERATION_START);
        }
    }

    default ResultOperation forceReload() {
        Thread threadOperation = getThreadOperation();
        if (threadOperation != null) {
            threadOperation.interrupt();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                throw new ForwardException(ie);
            }
        }
        return reload();
    }

    default void isNotRunThrow() {
        if (!getRun().get()) {
            throw new RuntimeException("isNotRun");
        }
    }

    default boolean isRun() {
        return getRun().get();
    }

}
