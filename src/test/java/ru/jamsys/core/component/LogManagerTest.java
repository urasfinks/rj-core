package ru.jamsys.core.component;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.item.Log;
import ru.jamsys.core.component.resource.LogManager;

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
        List<Map<String, Integer>> log = logManager.writeToFs("log");
        System.out.println(log);

//        LogManager.Item item1 = logManager.append(new HashMapBuilder<String, String>().append("test", "hello"), "world");
//        Assertions.assertEquals(1, logManager.getToFs().size());
//        logManager.write();
//        Assertions.assertEquals(0, logManager.getToFs().size());
//
//
//        //Assertions.assertEquals(0, loggerManager.getFromFs().size());
//        List<LogManager.Item> read = logManager.read();
//
//        Assertions.assertEquals(1, read.size());
//
//        LogManager.Item itemTimeEnvelopeMs = read.getFirst();
//
//        Assertions.assertEquals(item1.toString(), itemTimeEnvelopeMs.toString());
    }
}