package ru.jamsys.core.component;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.resource.item.Log;
import ru.jamsys.core.component.resource.LogComponent;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.util.UtilFile;

import java.util.List;
import java.util.Map;


//TODO: добавить тесты когда нет директории LogManager
class LogComponentTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        //App.main(args); мы не можем стартануть проект, так как запустится keepAlive
        // который будет сбрасывать счётчики tps и тесты будут разваливаться
        App.main(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void test() throws Exception {
        LogComponent logComponent = App.context.getBean(LogComponent.class);

        logComponent.append("log", new Log().setData("Hello world"));
        logComponent.append("log", new Log().setData("Hello world"));
        logComponent.append("log", new Log().setData("Hello world"));

        logComponent.getRateLimit().get(RateLimitName.FILE_LOG_SIZE.getName()).setMax(20);
        logComponent.getRateLimit().get(RateLimitName.FILE_LOG_INDEX.getName()).setMax(1);

        List<Map<String, Integer>> log = logComponent.writeToFs("log");
        Assertions.assertEquals(2, log.size());
        Assertions.assertEquals("LogManager/log.1.stop.bin", log.get(0).keySet().toArray()[0]);
        Assertions.assertEquals("LogManager/log.1.stop.bin", log.get(1).keySet().toArray()[0]);
        Assertions.assertEquals(34, log.get(0).get("LogManager/log.1.stop.bin"));
        Assertions.assertEquals(17, log.get(1).get("LogManager/log.1.stop.bin"));

        UtilFile.removeAllFilesInFolder("LogManager");

    }

    @Test
    void testSize() throws Exception {
        LogComponent logComponent = App.context.getBean(LogComponent.class);
        logComponent.getRateLimit().get(RateLimitName.FILE_LOG_SIZE.getName()).setMax(20);
        logComponent.getRateLimit().get(RateLimitName.FILE_LOG_INDEX.getName()).setMax(1);
        List<Map<String, Integer>> log;

        logComponent.append("log", new Log().setData("Hello world"));
        log = logComponent.writeToFs("log");
        Assertions.assertEquals(17, log.getFirst().get("LogManager/log.1.stop.bin"));

        logComponent.append("log", new Log().setData("Hello world").addHeader("test", "12345"));
        log = logComponent.writeToFs("log");
        Assertions.assertEquals(30, log.getFirst().get("LogManager/log.1.stop.bin"));

        UtilFile.removeAllFilesInFolder("LogManager");
    }

    @Test
    void bigWrite() throws Exception {
        LogComponent logComponent = App.context.getBean(LogComponent.class);

        logComponent.append("log", new Log().setData("a".repeat(20 * 1024 * 1024)));
        List<Map<String, Integer>> log = logComponent.writeToFs("log");
        Assertions.assertEquals(20971526, log.getFirst().get("LogManager/log.1.stop.bin"));

        UtilFile.removeAllFilesInFolder("LogManager");
    }
}