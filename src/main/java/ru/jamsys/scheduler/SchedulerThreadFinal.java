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
            listProcedure.forEach(Procedure::run);
            if (finalProcedure != null) {
                finalProcedure.run();
            }
        };
    }
}
