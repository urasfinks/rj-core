package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.App;

import java.util.ArrayList;
import java.util.List;

@Component
@Lazy
public class Core extends AbstractCoreComponent {

    List<Class<? extends AbstractCoreComponent>> init = new ArrayList<>();

    public Core() {
        init.add(Scheduler.class);
        init.add(Broker.class);
        init.add(StatisticAggregator.class);
    }

    @Override
    public void flushStatistic() {

    }

    public void run() {
        for (Class<? extends AbstractCoreComponent> cls : init) {
            App.context.getBean(cls).run();
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        //Опускаем в обратной последовательности
        for (int i = init.size() - 1; i >= 0; i--) {
            App.context.getBean(init.get(i)).shutdown();
        }
    }
}
