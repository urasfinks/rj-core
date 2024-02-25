package ru.jamsys.statistic;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PoolStatistic extends AbstractStatistic {

    String name;
    int size;
    int park;

}
