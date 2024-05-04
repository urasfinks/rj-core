package ru.jamsys.core.extension;

public interface RunnableInterface {

    void run();

    void shutdown();

    default void reload() {
        shutdown();
        run();
    }

}
