package ru.jamsys.core.extension;

public interface LifeCycleInterface {

    boolean isRun();

    void run();

    void shutdown();

    default void reload() {
        shutdown();
        run();
    }

}
