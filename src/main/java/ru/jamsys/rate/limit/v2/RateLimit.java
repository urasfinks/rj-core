package ru.jamsys.rate.limit.v2;

import java.util.Map;

public interface RateLimit {

    boolean isActive();

    Map<String, Object> flushTps(long curTime);

    @SuppressWarnings("unused")
    String getMomentumStatistic();

    void reset();

    boolean check(Integer limit);

}
