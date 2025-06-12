package ru.jamsys.core.extension;

import ru.jamsys.core.extension.statistic.StatisticDataHeader;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//Сбор статистики должен быть быстрым, не надо закладывать туда бизнес функции, которые влияют на функционал
public interface StatisticsFlush {

    List<StatisticDataHeader> flushAndGetStatistic(AtomicBoolean threadRun);

}
