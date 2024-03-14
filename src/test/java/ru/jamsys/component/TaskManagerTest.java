package ru.jamsys.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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
        calc = TaskManager.calc(map, 500);
        Assertions.assertEquals("{ReadStatisticSecTask=250, KeepAliveTask=167, FlushStatisticCollectorTask=83}", calc.toString(), "#1");

        map.clear();
        map.put("KeepAliveTask", 0L);
        map.put("FlushStatisticCollectorTask", 0L);
        calc = TaskManager.calc(map, 500);
        Assertions.assertEquals("{KeepAliveTask=250, FlushStatisticCollectorTask=250}", calc.toString(), "#2");

        map.clear();
        map.put("KeepAliveTask", 1L);
        map.put("FlushStatisticCollectorTask", 0L);
        calc = TaskManager.calc(map, 500);
        Assertions.assertEquals("{KeepAliveTask=250, FlushStatisticCollectorTask=250}", calc.toString(), "#3");

        map.clear();
        map.put("ReadStatisticSecTask", 0L);
        map.put("KeepAliveTask", 2L);
        map.put("FlushStatisticCollectorTask", 2L);
        calc = TaskManager.calc(map, 500);
        Assertions.assertEquals("{ReadStatisticSecTask=233, KeepAliveTask=133, FlushStatisticCollectorTask=133}", calc.toString(), "#3");
    }
}