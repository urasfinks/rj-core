package ru.jamsys.core.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.resource.LogManager;
import ru.jamsys.core.extension.HashMapBuilder;

import java.util.List;

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
//        LogManager logManager = App.context.getBean(LogManager.class);
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