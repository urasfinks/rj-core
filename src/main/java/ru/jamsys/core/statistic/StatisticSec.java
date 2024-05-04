package ru.jamsys.core.statistic;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.statistic.time.TimeControllerMsImpl;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StatisticSec extends TimeControllerMsImpl {

    private List<Statistic> list = new ArrayList<>();

}
