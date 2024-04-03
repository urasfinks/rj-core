package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.broker.BrokerCollectible;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StatisticSec extends TimeControllerImpl implements BrokerCollectible {

    private List<Statistic> list = new ArrayList<>();

}
