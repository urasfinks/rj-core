package ru.jamsys.core.rate.limit.item;

import ru.jamsys.core.extension.StatisticsFlush;

public interface RateLimitItem extends StatisticsFlush {

    boolean check(Integer limit);

    void set(Integer limit);

    long get();

    void reset(); //Используйте преимущественно для тестирования

    void inc();

    void dec();

}
