package ru.jamsys.core.extension;

import ru.jamsys.core.statistic.Statistic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

//Сбор статистики должен быть быстрым, не надо закладывать туда бизнес функции, которые влияют на функционал
public interface StatisticsFlush {

    List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean threadRun);

}
