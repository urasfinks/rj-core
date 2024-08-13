package ru.jamsys.core.extension.functional;

@SuppressWarnings("unused")
@FunctionalInterface
public interface ProcedureThrowing {

    void run() throws Throwable;

    @SuppressWarnings("unused")
    default ProcedureThrowing andThen(ProcedureThrowing after){
        return () -> {
            this.run();
            after.run();
        };
    }

    @SuppressWarnings("unused")
    default ProcedureThrowing compose(ProcedureThrowing before){
        return () -> {
            before.run();
            this.run();
        };
    }
}
