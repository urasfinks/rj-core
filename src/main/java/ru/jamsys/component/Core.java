package ru.jamsys.component;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.App;

import java.util.ArrayList;
import java.util.List;

@Component
@Lazy
public class Core extends AbstractCoreComponent {

    @Getter
    List<Class<? extends AbstractCoreComponent>> list = new ArrayList<>();

    public Core() {
        list.add(Scheduler.class);
        list.add(Broker.class);
        list.add(StatisticAggregator.class);
        list.add(StatisticSystem.class);
    }

    @Override
    public void flushStatistic() {

    }

    public void run(Class<? extends AbstractCoreComponent> statisticReader) {
        list.add(statisticReader);
        for (Class<? extends AbstractCoreComponent> cls : list) {
            App.context.getBean(cls).run();
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        //Опускаем в обратной последовательности
        for (int i = list.size() - 1; i >= 0; i--) {
            App.context.getBean(list.get(i)).shutdown();
        }
    }
}
