package ru.jamsys.core.statistic;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StatisticSec extends ExpirationMsMutableImpl {

    private List<Statistic> list = new ArrayList<>();

}
