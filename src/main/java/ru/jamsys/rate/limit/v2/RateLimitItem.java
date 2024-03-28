package ru.jamsys.rate.limit.v2;

import java.util.Map;

public interface RateLimitItem {

    boolean check(Integer limit);

    void setMax(Integer limit);

    long getMax();

    Map<String, Object> flushTps(long curTime);

    void reset(); //Используйте преимущественно для тестирования

}
