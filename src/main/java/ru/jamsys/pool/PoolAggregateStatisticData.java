package ru.jamsys.pool;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class PoolAggregateStatisticData {

    String name = getClass().getSimpleName();

    Map<String, PoolStatisticData> map = new HashMap<>();

}
