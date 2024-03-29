package ru.jamsys.rate.limit;

import ru.jamsys.extension.StatisticsCollector;

public interface RateLimitItem extends StatisticsCollector {

    boolean check(Integer limit);

    void setMax(Integer limit);

    long getMax();

    void reset(); //Используйте преимущественно для тестирования

}
