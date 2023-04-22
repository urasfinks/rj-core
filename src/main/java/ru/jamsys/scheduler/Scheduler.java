package ru.jamsys.scheduler;

import java.util.function.Consumer;

public interface Scheduler {

    void run();

    <T> Consumer<T> getConsumer();

    void shutdown();

    boolean isActive();

}
