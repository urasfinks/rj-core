package ru.jamsys.scheduler;

import lombok.Setter;
import ru.jamsys.Procedure;

import java.util.function.Consumer;

//Завершающий планировщик, у него есть специфичный finalProcedure
public class SchedulerThreadFinal extends SchedulerThreadImpl {

    @Setter
    public Procedure finalProcedure;

    public SchedulerThreadFinal(String name, long periodMillis) {
        super(name, periodMillis);
    }

    @Override
    public <T> Consumer<T> getConsumer() {
        return (t) -> {
            listProcedure.forEach((Procedure action) -> {
                long startTime = System.currentTimeMillis();
                action.run();
                execTime.add(System.currentTimeMillis() - startTime);
            });
            if (finalProcedure != null) {
                long startTime = System.currentTimeMillis();
                finalProcedure.run();
                execTime.add(System.currentTimeMillis() - startTime);
            }
        };
    }
}
