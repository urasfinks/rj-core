package ru.jamsys.scheduler;

import ru.jamsys.Procedure;
import ru.jamsys.statistic.Statistic;

import java.util.function.Consumer;

public interface SchedulerThread {

    void run();

    void shutdown();

    boolean isActive();

    void remove(Procedure procedure);

    void add(Procedure procedure);

    <T> Consumer<T> getConsumer();

    String getName();

}
