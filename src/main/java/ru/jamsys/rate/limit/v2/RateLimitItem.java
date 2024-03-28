package ru.jamsys.rate.limit.v2;

import java.util.Map;

public interface RateLimitItem {

    boolean check(Integer limit);

    void setMax(Integer limit);

    Map<String, Object> flushTps(long curTime);

}
