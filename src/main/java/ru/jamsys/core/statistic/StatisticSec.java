package ru.jamsys.core.statistic;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableImpl;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StatisticSec extends ExpiredMsMutableImpl {

    private List<Statistic> list = new ArrayList<>();

}
