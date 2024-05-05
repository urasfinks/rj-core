package ru.jamsys.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.HashMapBuilder;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;

class FSLoggerTest {
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        //App.main(args); мы не можем стартануть проект, так как запустится keepAlive
        // который будет сбрасывать счётчики tps и тесты будут разваливаться
        App.main(args);
    }

    @Test
    void test() throws Exception {
        FSLogger fsLogger = App.context.getBean(FSLogger.class);
        FSLogger.Item item1 = fsLogger.append(new HashMapBuilder<String, String>().append("test", "hello"), "world");
        Assertions.assertEquals(1, fsLogger.getToFs().size());
        fsLogger.write();
        Assertions.assertEquals(0, fsLogger.getToFs().size());

        Assertions.assertEquals(0, fsLogger.getFromFs().size());
        fsLogger.read();
        Assertions.assertEquals(1, fsLogger.getFromFs().size());

        TimeEnvelopeMs<FSLogger.Item> itemTimeEnvelopeMs = fsLogger.fromFs.pollFirst();

        Assertions.assertEquals(item1.toString(), itemTimeEnvelopeMs.getValue().toString());
    }
}