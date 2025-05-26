package ru.jamsys.core.extension;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public interface StatisticsCollectorMap<E extends StatisticsFlush> extends StatisticsFlush {

    Map<String, E> getMapForFlushStatistic();

    @Override
    default List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<DataHeader> result = new ArrayList<>();
        String clsName = App.getUniqueClassName(getClass());
        UtilRisc.forEach(
                threadRun,
                getMapForFlushStatistic(),
                (String _, E element) -> {
                    if(element != null){
                        List<DataHeader> statistics = element.flushAndGetStatistic(threadRun);
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
