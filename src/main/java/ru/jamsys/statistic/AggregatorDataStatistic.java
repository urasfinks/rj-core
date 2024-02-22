package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;

public class AggregatorDataStatistic<T> extends BrokerStatistic<T> {

    @Getter
    @Setter
    long timestamp = System.currentTimeMillis();

}
