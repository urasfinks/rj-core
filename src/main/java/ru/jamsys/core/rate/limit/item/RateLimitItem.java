package ru.jamsys.core.rate.limit.item;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.StatisticsFlush;

public interface RateLimitItem extends StatisticsFlush {

    String getNs(); // Получить пространство Property RateLimit

    boolean check(Integer limit);

    int get();

    default void set(int value) {
        App.get(ServiceProperty.class).setProperty(getNs(), value + "");
    }

    default void set(ApplicationContext applicationContext, int value) {
        applicationContext.getBean(ServiceProperty.class).setProperty(getNs(), value + "");
    }

}
