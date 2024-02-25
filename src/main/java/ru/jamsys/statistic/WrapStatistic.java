package ru.jamsys.statistic;

import lombok.Data;

@Data
public class WrapStatistic {

    Statistic statistic;
    Class<?> classOwner;

    public WrapStatistic(Class<?> classOwner, Statistic statistic) {
        this.statistic = statistic;
        this.classOwner = classOwner;
    }

}
