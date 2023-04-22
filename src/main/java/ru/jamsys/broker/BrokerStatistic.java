package ru.jamsys.broker;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BrokerStatistic {

    String name = getClass().getSimpleName();

    List<BrokerQueueStatistic> list = new ArrayList<>();

}
