package ru.jamsys.component;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import ru.jamsys.App;
import ru.jamsys.ApplicationInit;
import ru.jamsys.task.handler.DefaultReadStatistic;
import ru.jamsys.thread.Starter;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Component
@Lazy
public class Core extends AbstractComponent implements Starter {

    @Getter
    List<Class<? extends Starter>> list = new ArrayList<>();

    public Core(ApplicationContext applicationContext) {
        super(applicationContext);
        list.add(Generator.class);
    }

    public void run() {
        for (Class<? extends Starter> cls : list) {
            App.context.getBean(cls).run();
        }
    }

    public <T extends ApplicationInit> void applicationInit(Class<T> flushStatisticClass) {
        App.context.getBean(Security.class).run();
        if (flushStatisticClass != null) {
            App.context.getBean(flushStatisticClass).applicationInit();
        } else {
            App.context.getBean(DefaultReadStatistic.class).applicationInit();
        }
    }

    @Override
    public void shutdown() {
        //Опускаем в обратной последовательности
        for (int i = list.size() - 1; i >= 0; i--) {
            App.context.getBean(list.get(i)).shutdown();
        }
    }

    @Override
    synchronized public void reload() {
        shutdown();
        run();
    }
}
