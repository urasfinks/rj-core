package ru.jamsys.core.extension;

import ru.jamsys.core.extension.log.DataHeader;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//Сбор статистики должен быть быстрым, не надо закладывать туда бизнес функции, которые влияют на функционал
public interface StatisticsFlush {

    List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun);

}
