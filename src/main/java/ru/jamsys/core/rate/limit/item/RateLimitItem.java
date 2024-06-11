package ru.jamsys.core.rate.limit.item;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertyComponent;
import ru.jamsys.core.extension.StatisticsFlush;

public interface RateLimitItem extends StatisticsFlush {

    String getNs(); // Получить пространство Property RateLimit

    boolean check(Integer limit);

    int get();

    default void set(String prop, int value) {
        App.context.getBean(PropertyComponent.class).setProperty(getNs() + "." + prop, value + "");
    }

    default void set(ApplicationContext applicationContext, String prop, int value) {
        applicationContext.getBean(PropertyComponent.class).setProperty(getNs() + "." + prop, value + "");
    }

}
