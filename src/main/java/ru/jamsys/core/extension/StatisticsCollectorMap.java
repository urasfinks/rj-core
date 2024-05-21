package ru.jamsys.core.extension;

import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public interface StatisticsCollectorMap<T extends StatisticsFlush> extends StatisticsFlush {

    Map<String, T> getMapForFlushStatistic();

    @Override
    default List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        Map<String, T> map = getMapForFlushStatistic();
        List<Statistic> result = new ArrayList<>();
        String clsName = ClassNameImpl.getClassNameStatic(getClass(), null);
        UtilRisc.forEach(
                isThreadRun,
                map,
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
