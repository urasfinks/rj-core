package ru.jamsys.statistic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class ListStatistic<T> extends AbstractStatistic {

    List<T> list = new ArrayList<>();

}
