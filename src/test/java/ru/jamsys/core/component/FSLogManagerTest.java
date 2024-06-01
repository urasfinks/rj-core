package ru.jamsys.core.component;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.component.manager.item.LogWriter;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.rate.limit.RateLimitName;

import java.util.concurrent.atomic.AtomicBoolean;


//TODO: добавить тесты когда нет директории LogManager
class FSLogManagerTest {

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
//        LogComponent logComponent = App.context.getBean(LogComponent.class);
//
//        logComponent.append("log", new Log().setData("Hello world"));
//        logComponent.append("log", new Log().setData("Hello world"));
//        logComponent.append("log", new Log().setData("Hello world"));
//
//        logComponent.getRateLimit().get(RateLimitName.FILE_LOG_SIZE.getName()).setMax(20);
//        logComponent.getRateLimit().get(RateLimitName.FILE_LOG_INDEX.getName()).setMax(1);
//
//        List<Map<String, Integer>> log = logComponent.writeToFs("log");
//        Assertions.assertEquals(2, log.size());
//        Assertions.assertEquals("LogManager/log.1.stop.bin", log.get(0).keySet().toArray()[0]);
//        Assertions.assertEquals("LogManager/log.1.stop.bin", log.get(1).keySet().toArray()[0]);
//        Assertions.assertEquals(34, log.get(0).get("LogManager/log.1.stop.bin"));
//        Assertions.assertEquals(17, log.get(1).get("LogManager/log.1.stop.bin"));
//
//        UtilFile.removeAllFilesInFolder("LogManager");

    }

    @Test
    void testSize() throws Exception {
//        LogComponent logComponent = App.context.getBean(LogComponent.class);
//        logComponent.getRateLimit().get(RateLimitName.FILE_LOG_SIZE.getName()).setMax(20);
//        logComponent.getRateLimit().get(RateLimitName.FILE_LOG_INDEX.getName()).setMax(1);
//        List<Map<String, Integer>> log;
//
//        logComponent.append("log", new Log().setData("Hello world"));
//        log = logComponent.writeToFs("log");
//        Assertions.assertEquals(17, log.getFirst().get("LogManager/log.1.stop.bin"));
//
//        logComponent.append("log", new Log().setData("Hello world").addHeader("test", "12345"));
//        log = logComponent.writeToFs("log");
//        Assertions.assertEquals(30, log.getFirst().get("LogManager/log.1.stop.bin"));
//
//        UtilFile.removeAllFilesInFolder("LogManager");
    }

    @Test
    void bigWrite() throws Exception {
//        LogComponent logComponent = App.context.getBean(LogComponent.class);
//
//        logComponent.append("log", new Log().setData("a".repeat(20 * 1024 * 1024)));
//        List<Map<String, Integer>> log = logComponent.writeToFs("log");
//        Assertions.assertEquals(20971526, log.getFirst().get("LogManager/log.1.stop.bin"));
//
//        UtilFile.removeAllFilesInFolder("LogManager");
    }

    @Test
    void checkOverMaxFileWrite() {
        UtilFile.removeAllFilesInFolder("LogManager");
        LogWriter test = new LogWriter("default");
        test.setMaxFileSizeByte(1);
        test.setMaxFileCount(2);
        test.append(new Log("LogData1").addHeader("key", "value"));
        test.append(new Log("LogData2").addHeader("key", "value"));
        test.append(new Log("LogData3").addHeader("key", "value"));
        test.keepAlive(new AtomicBoolean(true));
        // Потому что за одну итерацию мы не записываем больше файлов чем максимальное кол-во
        Assertions.assertEquals(1, test.size());
    }

    @Test
    void checkTime() {
        UtilFile.removeAllFilesInFolder("LogManager");
        long start = System.currentTimeMillis();
        LogWriter test = new LogWriter("default");
        test.getBroker().get().getRateLimit().get(RateLimitName.BROKER_SIZE.getName()).set(9999999);
        long start2 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            test.append(new Log("LogData" + i).addHeader("key", "value"));
        }
        System.out.println("add time: " + (System.currentTimeMillis() - start2));
        long start3 = System.currentTimeMillis();
        test.keepAlive(new AtomicBoolean(true));
        System.out.println("write time: " + (System.currentTimeMillis() - start3));
        // Потому что за одну итерацию мы не записываем больше файлов чем максимальное кол-во
        System.out.println("all time: " + (System.currentTimeMillis() - start));
    }
}