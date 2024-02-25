package ru.jamsys.statistic;

import java.util.ArrayList;
import java.util.List;

public interface Statistic {

    @SuppressWarnings("unused")
    Class<?> getInstance();

    default List<StatisticEntity> getStatisticEntity() {
        return new ArrayList<>();
    }

}
