package ru.jamsys.extension;

import ru.jamsys.statistic.Statistic;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface StatisticsCollectorMap<T extends StatisticsCollector> extends StatisticsCollector {

    Map<String, T> getMapStatisticCollectorMap();

    @Override
    default List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, ThreadEnvelope threadEnvelope){
        Map<String, T> map = getMapStatisticCollectorMap();
        List<Statistic> result = new ArrayList<>();
        String clsName = getClass().getSimpleName();
        Util.riskModifierMap(
                threadEnvelope.getIsWhile(),
                map,
                new String[0],
                (String key, T element) -> {
                    LinkedHashMap<String, String> newParentTags = new LinkedHashMap<>(parentTags);
                    newParentTags.put(clsName, key);
                    List<Statistic> statistics = element.flushAndGetStatistic(newParentTags, parentFields, threadEnvelope);
                    if (statistics != null) {
                        result.addAll(statistics);
                    }
                }
        );
        return result;
    }
}
