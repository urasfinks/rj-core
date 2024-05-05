package ru.jamsys.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.resource.LoggerManager;
import ru.jamsys.core.extension.HashMapBuilder;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;

class LoggerManagerTest {
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        //App.main(args); мы не можем стартануть проект, так как запустится keepAlive
        // который будет сбрасывать счётчики tps и тесты будут разваливаться
        App.main(args);
    }

    @Test
    void test() throws Exception {
        LoggerManager loggerManager = App.context.getBean(LoggerManager.class);
        LoggerManager.Item item1 = loggerManager.append(new HashMapBuilder<String, String>().append("test", "hello"), "world");
        Assertions.assertEquals(1, loggerManager.getToFs().size());
        loggerManager.write();
        Assertions.assertEquals(0, loggerManager.getToFs().size());

        Assertions.assertEquals(0, loggerManager.getFromFs().size());
        loggerManager.read();
        Assertions.assertEquals(1, loggerManager.getFromFs().size());

        TimeEnvelopeMs<LoggerManager.Item> itemTimeEnvelopeMs = loggerManager.fromFs.pollFirst();

        Assertions.assertEquals(item1.toString(), itemTimeEnvelopeMs.getValue().toString());
    }
}