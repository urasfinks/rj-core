package ru.jamsys.core.extension;

import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public interface StatisticsCollectorMap<T extends StatisticsFlush> extends StatisticsFlush {

    Map<String, T> getMapStatisticCollectorMap();

    @Override
    default List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun){
        Map<String, T> map = getMapStatisticCollectorMap();
        List<Statistic> result = new ArrayList<>();
        String clsName = getClass().getSimpleName();
        Util.riskModifierMap(
                isThreadRun,
                map,
                new String[0],
                (String key, T element) -> {
                    LinkedHashMap<String, String> newParentTags = new LinkedHashMap<>(parentTags);
                    newParentTags.put(clsName, key);
                    List<Statistic> statistics = element.flushAndGetStatistic(newParentTags, parentFields, isThreadRun);
                    if (statistics != null) {
                        result.addAll(statistics);
                    }
                }
        );
        return result;
    }
}