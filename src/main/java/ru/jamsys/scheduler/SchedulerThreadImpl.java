package ru.jamsys.scheduler;

import lombok.Setter;
import ru.jamsys.Procedure;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SchedulerThreadImpl extends AbstractSchedulerThread {

    final CopyOnWriteArrayList<Procedure> listProcedure = new CopyOnWriteArrayList<>();

    public SchedulerThreadImpl(String name, long periodMillis) {
        super(name, periodMillis);
        run();
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
        return (t) -> listProcedure.forEach(Procedure::run);
    }

}
