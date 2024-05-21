package ru.jamsys.core.extension;

public interface LifeCycleInterface {

    void run();

    void shutdown();

    default void reload() {
        shutdown();
        run();
    }

}
