package ru.jamsys.statistic;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StatisticSec extends TimeControllerImpl {

    private List<Statistic> list = new ArrayList<>();

}
