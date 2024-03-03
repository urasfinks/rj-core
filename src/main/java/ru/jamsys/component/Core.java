package ru.jamsys.component;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;

import ru.jamsys.App;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Component
@Lazy
public class Core extends AbstractComponent {

    @Getter
    List<Class<? extends Component>> list = new ArrayList<>();

    public Core() {
        //list.add(Security.class);
        //list.add(Scheduler.class);
        //list.add(Broker.class);
        //list.add(StatisticAggregator.class);
        //list.add(StatisticSystem.class);
        list.add(ExecutorService.class);
        list.add(SystemStatistic.class);
    }

    public void run(Class<? extends Component> statisticReader) {
        if (statisticReader != null) {
            list.add(statisticReader);
        }
        for (Class<? extends Component> cls : list) {
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
