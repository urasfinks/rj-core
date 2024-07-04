package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.item.FileByteWriter;
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.component.manager.item.LogType;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.StatisticSec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

// IO time: 1sec 425ms
// COMPUTE time: 1sec 411ms

//TODO: добавить тесты когда нет директории LogManager
class FileByteWriterTest {

    @BeforeAll
    static void beforeAll() {
        UtilFile.removeAllFilesInFolder("LogManager");
        String[] args = new String[]{"run.args.remote.log=false"};
        //App.main(args); мы не можем стартануть проект, так как запустится keepAlive
        // который будет сбрасывать счётчики tps и тесты будут разваливаться
        //App.main(args);
        App.context = SpringApplication.run(App.class, args);
        App.context.getBean(ServiceProperty.class).setProperty("run.args.remote.log", "false");
    }

    @AfterAll
    static void shutdown() {
        UtilFile.removeAllFilesInFolder("LogManager");
        App.shutdown();
    }

    @Test
    void checkOverMaxFileWrite() {
        UtilFile.removeAllFilesInFolder("LogManager");
        FileByteWriter test = new FileByteWriter("default1", App.context);

        test.setProperty("log.file.size.kb", "1");
        test.setProperty("log.file.count", "2");

        test.append(new Log(LogType.INFO).setData("LogData1"));
        test.append(new Log(LogType.INFO).setData("LogData2"));
        test.append(new Log(LogType.INFO).setData("LogData3"));
        test.keepAlive(new AtomicBoolean(true));
        // Потому что за одну итерацию мы не записываем больше файлов чем максимальное кол-во
        // Ничего личного, просто такие правила
        Assertions.assertEquals(1, test.size());

        Assertions.assertEquals("[/default1.0.bin, /default1.1.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        // Должна произойти перезапись 0 файла
        test.keepAlive(new AtomicBoolean(true));
        Assertions.assertEquals(0, test.size());
        Assertions.assertEquals("[/default1.0.bin, /default1.1.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
    }

    @Test
    void checkNameLog() {

        UtilFile.removeAllFilesInFolder("LogManager");
        FileByteWriter test = new FileByteWriter("default2", App.context);

        test.setProperty("log.file.count", "100");

        test.append(new Log(LogType.INFO).setData("LogData1"));
        test.append(new Log(LogType.INFO).setData("LogData2"));
        test.append(new Log(LogType.INFO).setData("LogData3"));
        test.keepAlive(new AtomicBoolean(true));

        Assertions.assertEquals("[/default2.000.proc.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
        test.close();
        Assertions.assertEquals("[/default2.000.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
    }

    @Test
    void checkRestoreExceptionShutdown() throws IOException {
        UtilFile.removeAllFilesInFolder("LogManager");

        UtilFile.writeBytes("LogManager/default3.000.bin", "hello1".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
        UtilFile.writeBytes("LogManager/default3.001.bin", "hello2".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
        UtilFile.writeBytes("LogManager/default3.002.proc.bin", "hello3".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);

        // Файлы для негативных проверок
        UtilFile.writeBytes("LogManager/test.003.proc.bin", "hello3".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
        UtilFile.writeBytes("LogManager/test.004.bin", "hello3".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);

        Assertions.assertEquals("[/default3.000.bin, /default3.001.bin, /default3.002.proc.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        FileByteWriter test = new FileByteWriter("default3", App.context);
        // Проверяем, что default3.002.proc.bin - удалён
        Assertions.assertEquals("[/default3.000.bin, /default3.001.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        Assertions.assertEquals(2, test.getIndexFile());

        test.append(new Log(LogType.INFO).setData("LogData1"));
        test.keepAlive(new AtomicBoolean(true));
        Assertions.assertEquals("[/default3.000.bin, /default3.001.bin, /default3.002.proc.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        test.close();
        Assertions.assertEquals("[/default3.000.bin, /default3.001.bin, /default3.002.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
    }

    @Test
    void checkTime() {
        UtilFile.removeAllFilesInFolder("LogManager");
        long start = System.currentTimeMillis();
        FileByteWriter test = new FileByteWriter("default4", App.context);
        //test.getBroker().getRateLimit().get(RateLimitName.BROKER_SIZE.getName()).set(9999999);
        test.getBroker().setMaxSizeQueue(9999999);
        long start2 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            test.append(new Log(LogType.INFO).setData("LogData" + i));
        }
        System.out.println("add time: " + (System.currentTimeMillis() - start2));
        long start3 = System.currentTimeMillis();
        test.keepAlive(new AtomicBoolean(true));
        System.out.println("write time: " + (System.currentTimeMillis() - start3));
        // Потому что за одну итерацию мы не записываем больше файлов чем максимальное кол-во
        System.out.println("all time: " + (System.currentTimeMillis() - start));
    }

    @Test
    void test() throws Exception {
        Log log1 = new Log(LogType.INFO).setData("Hello");
        byte[] x = log1.getByteInstance();

        Log log2 = new Log(LogType.INFO);
        log2.instanceFromByte(x);
        System.out.println(log2);
        Assertions.assertEquals(log1.toString(), log2.toString());

    }

    @Test
    void serialize() {
        StatisticSec statisticSec1 = new StatisticSec();
        statisticSec1.getList().add(new Statistic().addField("f1", 1).addTag("t1", "Hello"));
        byte[] byteInstance = statisticSec1.getByteInstance();

        StatisticSec statisticSec2 = new StatisticSec();
        statisticSec2.instanceFromByte(byteInstance);

        Assertions.assertEquals(statisticSec1.toString(), statisticSec2.toString());
    }

    @Test
    void serializeStatisticSecToFile() {
        UtilFile.removeAllFilesInFolder("LogManager");
        StatisticSec statisticSec1 = new StatisticSec();
        statisticSec1.getList().add(new Statistic().addField("f1", 1).addTag("t1", "Hello"));
        FileByteWriter test = new FileByteWriter("default5", App.context);
        test.append(statisticSec1);
        test.keepAlive(new AtomicBoolean(true));
    }

}