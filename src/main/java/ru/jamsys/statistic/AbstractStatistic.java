package ru.jamsys.statistic;

import lombok.Data;

@Data
public abstract class AbstractStatistic implements Statistic{
    final String name = getClass().getSimpleName();
}
