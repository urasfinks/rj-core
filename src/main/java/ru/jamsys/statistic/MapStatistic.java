package ru.jamsys.statistic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class MapStatistic<K, V> extends AbstractStatistic {

    Map<K, V> map = new LinkedHashMap<>();

}
