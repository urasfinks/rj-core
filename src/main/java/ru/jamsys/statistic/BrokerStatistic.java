package ru.jamsys.statistic;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class BrokerStatistic<T> extends AbstractStatistic {

    @Getter
    List<T> list = new ArrayList<>();

}
