package ru.jamsys.statistic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public interface StatisticsCollector {
    List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun);
}