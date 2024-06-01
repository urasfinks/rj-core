package ru.jamsys.core.component;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.component.manager.item.LogWriter;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.rate.limit.RateLimitName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        UtilFile.removeAllFilesInFolder("LogManager");
        App.shutdown();
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
        // Ничего личного, просто такие правила
        Assertions.assertEquals(1, test.size());

        Assertions.assertEquals("[/default.0.bin, /default.1.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        // Должна произойти перезапись 0 файла
        test.keepAlive(new AtomicBoolean(true));
        Assertions.assertEquals(0, test.size());
        Assertions.assertEquals("[/default.0.bin, /default.1.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
    }

    @Test
    void checkNameLog() {
        UtilFile.removeAllFilesInFolder("LogManager");
        LogWriter test = new LogWriter("default");
        test.setMaxFileCount(100);
        test.append(new Log("LogData1").addHeader("key", "value"));
        test.append(new Log("LogData2").addHeader("key", "value"));
        test.append(new Log("LogData3").addHeader("key", "value"));
        test.keepAlive(new AtomicBoolean(true));

        Assertions.assertEquals("[/default.000.proc.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
        test.shutdown();
        Assertions.assertEquals("[/default.000.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
    }

    @Test
    void checkRestoreExceptionShutdown() throws IOException {
        UtilFile.removeAllFilesInFolder("LogManager");

        UtilFile.writeBytes("LogManager/default.000.bin", "hello1".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
        UtilFile.writeBytes("LogManager/default.001.bin", "hello2".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
        UtilFile.writeBytes("LogManager/default.002.proc.bin", "hello3".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);

        // Файлы для негативных проверок
        UtilFile.writeBytes("LogManager/test.003.proc.bin", "hello3".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
        UtilFile.writeBytes("LogManager/test.004.bin", "hello3".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);

        Assertions.assertEquals("[/default.000.bin, /default.001.bin, /default.002.proc.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        LogWriter test = new LogWriter("default");

        Assertions.assertEquals("[/default.000.bin, /default.001.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        Assertions.assertEquals(2, test.getIndexFile());

        test.append(new Log("LogData1").addHeader("key", "value"));
        test.keepAlive(new AtomicBoolean(true));
        Assertions.assertEquals("[/default.000.bin, /default.001.bin, /default.002.proc.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        test.shutdown();
        Assertions.assertEquals("[/default.000.bin, /default.001.bin, /default.002.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
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