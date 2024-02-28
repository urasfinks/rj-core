package ru.jamsys.scheduler;

import ru.jamsys.Procedure;
import ru.jamsys.statistic.AvgMetric;
import ru.jamsys.statistic.SchedulerThreadStatistic;
import ru.jamsys.statistic.Statistic;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SchedulerThreadImpl extends AbstractSchedulerThread {

    final CopyOnWriteArrayList<Procedure> listProcedure = new CopyOnWriteArrayList<>();
    AvgMetric timeExecute = new AvgMetric();

    public SchedulerThreadImpl(String name, long periodMillis) {
        super(name, periodMillis);
    }

    @Override
    public void add(Procedure procedure) {
        if (procedure != null) {
            listProcedure.addIfAbsent(procedure);
        }
    }

    @Override
    public void remove(Procedure procedure) {
        listProcedure.remove(procedure);
        if (listProcedure.isEmpty()) {
            shutdown();
        }
    }

    @Override
    public <T> Consumer<T> getConsumer() {
        return (t) -> listProcedure.forEach((Procedure action) -> {
            long startTime = System.currentTimeMillis();
            action.run();
            timeExecute.add(System.currentTimeMillis() - startTime);
        });
    }

    @Override
    public Statistic flushAndGetStatistic() {
        return new SchedulerThreadStatistic(timeExecute.flush());
    }

}
