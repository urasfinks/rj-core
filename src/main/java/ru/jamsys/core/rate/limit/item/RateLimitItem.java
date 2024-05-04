package ru.jamsys.core.rate.limit.item;

import ru.jamsys.core.extension.StatisticsFlush;

public interface RateLimitItem extends StatisticsFlush {

    boolean check(Integer limit);

    void setMax(Integer limit);

    long getMax();

    void reset(); //Используйте преимущественно для тестирования

    void incrementMax();

    String getMomentumStatistic();

}
