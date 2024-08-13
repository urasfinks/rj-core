package ru.jamsys.core.extension.functional;

@SuppressWarnings("unused")
@FunctionalInterface
public interface Procedure {

    void run() throws Throwable;

    @SuppressWarnings("unused")
    default Procedure andThen(Procedure after){
        return () -> {
            this.run();
            after.run();
        };
    }

    @SuppressWarnings("unused")
    default Procedure compose(Procedure before){
        return () -> {
            before.run();
            this.run();
        };
    }
}
