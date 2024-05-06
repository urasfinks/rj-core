package ru.jamsys.core.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.item.Log;
import ru.jamsys.core.component.resource.LogManager;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.util.UtilFile;

import java.util.List;
import java.util.Map;

class LogManagerTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        //App.main(args); мы не можем стартануть проект, так как запустится keepAlive
        // который будет сбрасывать счётчики tps и тесты будут разваливаться
        App.main(args);

    }

    @Test
    void test() throws Exception {
        LogManager logManager = App.context.getBean(LogManager.class);

        logManager.append("log", new Log().setData("Hello world"));
        logManager.append("log", new Log().setData("Hello world"));
        logManager.append("log", new Log().setData("Hello world"));

        logManager.getRateLimit().get(RateLimitName.FILE_LOG_SIZE.getName()).setMax(20);
        logManager.getRateLimit().get(RateLimitName.FILE_LOG_INDEX.getName()).setMax(1);

        List<Map<String, Integer>> log = logManager.writeToFs("log");
        Assertions.assertEquals(2, log.size());
        Assertions.assertEquals("LogManager/log.1.stop.bin", log.get(0).keySet().toArray()[0]);
        Assertions.assertEquals("LogManager/log.1.stop.bin", log.get(1).keySet().toArray()[0]);
        Assertions.assertEquals(34, log.get(0).get("LogManager/log.1.stop.bin"));
        Assertions.assertEquals(17, log.get(1).get("LogManager/log.1.stop.bin"));

        UtilFile.removeAllFilesInFolder("LogManager");

    }

    @Test
    void testSize() throws Exception {
        LogManager logManager = App.context.getBean(LogManager.class);
        logManager.getRateLimit().get(RateLimitName.FILE_LOG_SIZE.getName()).setMax(20);
        logManager.getRateLimit().get(RateLimitName.FILE_LOG_INDEX.getName()).setMax(1);
        List<Map<String, Integer>> log;

        logManager.append("log", new Log().setData("Hello world"));
        log = logManager.writeToFs("log");
        Assertions.assertEquals(17, log.getFirst().get("LogManager/log.1.stop.bin"));

        logManager.append("log", new Log().setData("Hello world").addHeader("test", "12345"));
        log = logManager.writeToFs("log");
        Assertions.assertEquals(30, log.getFirst().get("LogManager/log.1.stop.bin"));

        UtilFile.removeAllFilesInFolder("LogManager");
    }

    @Test
    void bigWrite() throws Exception {
        LogManager logManager = App.context.getBean(LogManager.class);

        logManager.append("log", new Log().setData("a".repeat(20 * 1024 * 1024)));
        List<Map<String, Integer>> log = logManager.writeToFs("log");
        Assertions.assertEquals(20971526, log.getFirst().get("LogManager/log.1.stop.bin"));

        UtilFile.removeAllFilesInFolder("LogManager");
    }
}