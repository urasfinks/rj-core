package ru.jamsys.component;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;

import ru.jamsys.App;
import ru.jamsys.ApplicationInit;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Component
@Lazy
public class Core extends AbstractComponent {

    @Getter
    List<Class<? extends Component>> list = new ArrayList<>();

    public Core(ApplicationContext applicationContext) {
        super(applicationContext);
        list.add(Scheduler.class);
        list.add(SystemStatistic.class);
    }

    public void run() {
        for (Class<? extends Component> cls : list) {
            App.context.getBean(cls).run();
        }
    }

    public <T extends ApplicationInit> void applicationInit(@NonNull Class<T> flushStatisticClass) {
        App.context.getBean(Security.class).run();
        App.context.getBean(flushStatisticClass).applicationInit();
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
