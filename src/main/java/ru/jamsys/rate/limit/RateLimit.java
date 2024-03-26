package ru.jamsys.rate.limit;

import java.util.Map;

public interface RateLimit {

    boolean isActive();

    Map<String, Object> flush();

    @SuppressWarnings("unused")
    String getMomentumStatistic();

    void reset();

}
