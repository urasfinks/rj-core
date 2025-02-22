package ru.jamsys.core.extension;

import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.statistic.Statistic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public interface StatisticsCollectorMap<E extends StatisticsFlush> extends StatisticsFlush {

    Map<String, E> getMapForFlushStatistic();

    @Override
    default List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean threadRun) {
        List<Statistic> result = new ArrayList<>();
        String clsName = App.getUniqueClassName(getClass());
        UtilRisc.forEach(
                threadRun,
                getMapForFlushStatistic(),
                (String key, E element) -> {
                    //TODO: почему то element бывает = null
                    if(element != null){
                        LinkedHashMap<String, String> newParentTags = new LinkedHashMap<>(parentTags);
                        newParentTags.put(clsName, key);
                        List<Statistic> statistics = element.flushAndGetStatistic(newParentTags, parentFields, threadRun);
                        if (statistics != null) {
                            result.addAll(statistics);
                        }
                    } else {
                        App.error(new RuntimeException("element is null; class: " + clsName));
                    }
                }
        );
        return result;
    }
}
