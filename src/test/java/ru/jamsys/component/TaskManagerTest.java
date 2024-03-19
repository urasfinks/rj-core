package ru.jamsys.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

class TaskManagerTest {

    @Test
    void keepAlive() {
        Map<String, Long> map = new LinkedHashMap<>();
        Map<String, Long> calc;

        map.clear();
        map.put("ReadStatisticSecTask", 1000L);
        map.put("KeepAliveTask", 2000L);
        map.put("FlushStatisticCollectorTask", 3000L);
        calc = TaskManager.getCountThreadByTime(map, 500);
        Assertions.assertEquals("{ReadStatisticSecTask=272, KeepAliveTask=136, FlushStatisticCollectorTask=90}", calc.toString(), "#1");

        map.clear();
        map.put("KeepAliveTask", 0L);
        map.put("FlushStatisticCollectorTask", 0L);
        calc = TaskManager.getCountThreadByTime(map, 500);
        Assertions.assertEquals("{KeepAliveTask=250, FlushStatisticCollectorTask=250}", calc.toString(), "#2");

        map.clear();
        map.put("KeepAliveTask", 1L);
        map.put("FlushStatisticCollectorTask", 0L);
        calc = TaskManager.getCountThreadByTime(map, 500);
        Assertions.assertEquals("{KeepAliveTask=250, FlushStatisticCollectorTask=250}", calc.toString(), "#3");

        map.clear();
        map.put("ReadStatisticSecTask", 0L);
        map.put("KeepAliveTask", 2L);
        map.put("FlushStatisticCollectorTask", 2L);
        calc = TaskManager.getCountThreadByTime(map, 500);
        Assertions.assertEquals("{ReadStatisticSecTask=250, KeepAliveTask=125, FlushStatisticCollectorTask=125}", calc.toString(), "#3");

        map.clear();
        map.put("ReadStatisticSecTask", 0L);
        map.put("KeepAliveTask", 506L);
        map.put("FlushStatisticCollectorTask", 1L);
        calc = TaskManager.getCountThreadByTime(map, 500);
        Assertions.assertEquals("{ReadStatisticSecTask=249, KeepAliveTask=1, FlushStatisticCollectorTask=249}", calc.toString(), "#3");

        map.clear();
        map.put("KeepAliveTask", 0L);
        map.put("FlushStatisticCollectorTask", 0L);
        calc = TaskManager.getCountThreadByTime(map, 500);
        Assertions.assertEquals("{KeepAliveTask=250, FlushStatisticCollectorTask=250}", calc.toString(), "#3");

    }
}