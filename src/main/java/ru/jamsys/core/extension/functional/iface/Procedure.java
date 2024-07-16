package ru.jamsys.core.extension.functional.iface;

@SuppressWarnings("unused")
@FunctionalInterface
public interface Procedure {

    void run();

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
