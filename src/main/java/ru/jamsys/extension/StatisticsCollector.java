package ru.jamsys.extension;

import ru.jamsys.statistic.Statistic;
import ru.jamsys.thread.ThreadEnvelope;

import java.util.List;
import java.util.Map;

//Сбор статистики должен быть быстрым, не надо закладывать туда бизнес функции, которые влияют на функционал
public interface StatisticsCollector {

    List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, ThreadEnvelope threadEnvelope);

}
