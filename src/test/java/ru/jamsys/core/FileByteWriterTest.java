package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerFileByteWriter;
import ru.jamsys.core.component.manager.item.BrokerProperty;
import ru.jamsys.core.component.manager.item.FileByteProperty;
import ru.jamsys.core.component.manager.item.FileByteWriter;
import ru.jamsys.core.component.manager.item.log.PersistentData;
import ru.jamsys.core.component.manager.item.log.PersistentDataHeader;
import ru.jamsys.core.component.manager.item.log.LogType;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.StatisticSec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

// IO time: 1sec 425ms
// COMPUTE time: 1sec 411ms

class FileByteWriterTest {

    @BeforeAll
    static void beforeAll() {
        UtilFile.removeAllFilesInFolder("LogManager");
        App.getRunBuilder().addTestArguments().runSpring();
    }

    @AfterAll
    static void shutdown() {
        UtilFile.removeAllFilesInFolder("LogManager");
        App.shutdown();
    }

    @Test
    void folderNotExist() {
        try {
            FileByteWriter test = App.get(ManagerFileByteWriter.class).get("checkOverMaxFileWrite", PersistentDataHeader.class);
            App.get(ServiceProperty.class).set(
                    test
                            .getPropertyDispatcher()
                            .getPropertyRepository()
                            .getByFieldNameConstants(FileByteProperty.Fields.folder)
                            .getPropertyKey(),
                    "xxkaa"
            );
            Assertions.fail();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Test
    void checkOverMaxFileWrite() {
        App.get(ServiceProperty.class).set(
                "App.ManagerFileByteWriter.checkOverMaxFileWrite<Log>.file.name",
                "log"
        );

        UtilFile.removeAllFilesInFolder("LogManager");
        FileByteWriter test = App.get(ManagerFileByteWriter.class).get("checkOverMaxFileWrite", PersistentData.class);

        App.get(ServiceProperty.class).set(
                test
                        .getPropertyDispatcher()
                        .getPropertyRepository()
                        .getByFieldNameConstants(FileByteProperty.Fields.fileSizeKb)
                        .getPropertyKey(),
                "1"
        );
        App.get(ServiceProperty.class).set(
                test
                        .getPropertyDispatcher()
                        .getPropertyRepository()
                        .getByFieldNameConstants(FileByteProperty.Fields.fileCount)
                        .getPropertyKey(),
                "2"
        );
        App.get(ServiceProperty.class).set(
                test
                        .getPropertyDispatcher()
                        .getPropertyRepository()
                        .getByFieldNameConstants(FileByteProperty.Fields.fileName)
                        .getPropertyKey(),
                "default1"
        );

        //UtilLog.printInfo(FileByteWriter.class, test.getPropertyDispatcher());

        test.append(UtilLog.info(FileByteWriterTest.class, "LogData1"));
        test.append(UtilLog.info(FileByteWriterTest.class, "LogData2"));
        test.append(UtilLog.info(FileByteWriterTest.class, "LogData3"));
        test.flush(new AtomicBoolean(true));
        // Потому что за одну итерацию мы не записываем больше файлов чем максимальное кол-во
        // Ничего личного, просто такие правила
        Assertions.assertEquals(1, test.getBroker().size());

        Assertions.assertEquals("[/default1.0.bin, /default1.1.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        // Должна произойти перезапись 0 файла
        test.flush(new AtomicBoolean(true));
        Assertions.assertEquals(0, test.getBroker().size());
        Assertions.assertEquals("[/default1.0.bin, /default1.1.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
    }

    @Test
    void checkNameLog() {
        App.get(ServiceProperty.class).set(
                "App.ManagerFileByteWriter.checkNameLog<Log>.file.name",
                "log"
        );

        UtilFile.removeAllFilesInFolder("LogManager");
        FileByteWriter test = App.get(ManagerFileByteWriter.class).get("checkNameLog", PersistentData.class);

        App.get(ServiceProperty.class).set(
                test
                        .getPropertyDispatcher()
                        .getPropertyRepository()
                        .getByFieldNameConstants(FileByteProperty.Fields.fileCount)
                        .getPropertyKey(),
                "100"
        );
        App.get(ServiceProperty.class).set(
                test
                        .getPropertyDispatcher()
                        .getPropertyRepository()
                        .getByFieldNameConstants(FileByteProperty.Fields.fileName)
                        .getPropertyKey(),
                "default2"
        );

        test.append(new PersistentDataHeader(LogType.INFO, FileByteWriterTest.class, "LogData1"));
        test.append(new PersistentDataHeader(LogType.INFO, FileByteWriterTest.class, "LogData2"));
        test.append(new PersistentDataHeader(LogType.INFO, FileByteWriterTest.class, "LogData3"));
        test.flush(new AtomicBoolean(true));

        Assertions.assertEquals("[/default2.000.proc.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
        test.shutdown();
        Assertions.assertEquals("[/default2.000.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
    }

    @Test
    void checkRestoreExceptionShutdown() throws IOException {
        App.get(ServiceProperty.class).set(
                "App.ManagerFileByteWriter.checkRestoreExceptionShutdown<Log>.file.name",
                "log"
        );
        UtilFile.removeAllFilesInFolder("LogManager");

        UtilFile.writeBytes("LogManager/default3.000.bin", "hello1".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
        UtilFile.writeBytes("LogManager/default3.001.bin", "hello2".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
        UtilFile.writeBytes("LogManager/default3.002.proc.bin", "hello3".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);

        // Файлы для негативных проверок
        UtilFile.writeBytes("LogManager/test.003.proc.bin", "hello3".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
        UtilFile.writeBytes("LogManager/test.004.bin", "hello3".getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);

        Assertions.assertEquals("[/default3.000.bin, /default3.001.bin, /default3.002.proc.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        FileByteWriter test = App.get(ManagerFileByteWriter.class).get("checkRestoreExceptionShutdown", PersistentData.class);
        //test.getPropertySubscriber().getPropertyRepository().setRepository("file.name", "default3");
        App.get(ServiceProperty.class).set(
                "App.ManagerFileByteWriter.checkRestoreExceptionShutdown<Log>.file.name",
               "default3"
        );
        // Проверяем, что default3.002.proc.bin - удалён
        Assertions.assertEquals("[/default3.000.bin, /default3.001.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        Assertions.assertEquals(2, test.getIndexFile());

        test.append(new PersistentDataHeader(LogType.INFO, FileByteWriterTest.class, "LogData1"));
        test.flush(new AtomicBoolean(true));
        Assertions.assertEquals("[/default3.000.bin, /default3.001.bin, /default3.002.proc.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());

        test.shutdown();
        Assertions.assertEquals("[/default3.000.bin, /default3.001.bin, /default3.002.bin, /test.003.proc.bin, /test.004.bin]", UtilFile.getFilesRecursive("LogManager", false).toString());
    }

    @Test
    void checkTime() {
        App.get(ServiceProperty.class).set(
                "App.ManagerFileByteWriter.checkTime<Log>.file.name",
                "log"
        );
        UtilFile.removeAllFilesInFolder("LogManager");
        long start = System.currentTimeMillis();
        FileByteWriter test = App.get(ManagerFileByteWriter.class).get("checkTime", PersistentData.class);

        App.get(ServiceProperty.class).set(
                "App.ManagerFileByteWriter.checkRestoreExceptionShutdown<Log>.file.name",
                "default4"
        );

        //test.getBroker().getRateLimit().get(RateLimitName.BROKER_SIZE.getName()).set(9999999);

        App.get(ServiceProperty.class).set(
                test
                        .getBroker()
                        .getPropertyDispatcher()
                        .getPropertyRepository()
                        .getByFieldNameConstants(BrokerProperty.Fields.size)
                        .getPropertyKey(),
                9999999
        );

        long start2 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            test.append(new PersistentDataHeader(LogType.INFO, FileByteWriterTest.class, "LogData" + i));
        }
        System.out.println("add time: " + (System.currentTimeMillis() - start2));
        long start3 = System.currentTimeMillis();
        test.flush(new AtomicBoolean(true));
        System.out.println("write time: " + (System.currentTimeMillis() - start3));
        // Потому что за одну итерацию мы не записываем больше файлов чем максимальное кол-во
        System.out.println("all time: " + (System.currentTimeMillis() - start));
    }

    @Test
    void test() throws Exception {
        PersistentDataHeader log1 = new PersistentDataHeader(LogType.INFO, FileByteWriterTest.class, "Hello");
        byte[] x = log1.toByte();

        PersistentDataHeader log2 = PersistentDataHeader.instanceFromBytes(x, (short) 0);
        System.out.println(log2);
        Assertions.assertEquals(log1.toString(), log2.toString());

    }

    @Test
    void serialize() {
        StatisticSec statisticSec1 = new StatisticSec();
        statisticSec1.getList().add(new Statistic().addField("f1", 1).addTag("t1", "Hello"));
        byte[] byteInstance = statisticSec1.toByte();

        StatisticSec statisticSec2 = StatisticSec.instanceFromBytes(byteInstance);
        Assertions.assertEquals(statisticSec1.toString(), statisticSec2.toString());
    }

    @Test
    void serializeStatisticSecToFile() {
        App.get(ServiceProperty.class).set(
                "App.ManagerFileByteWriter.default5<Log>.file.name",
                "log"
        );
        UtilFile.removeAllFilesInFolder("LogManager");
        StatisticSec statisticSec1 = new StatisticSec();
        statisticSec1.getList().add(new Statistic().addField("f1", 1).addTag("t1", "Hello"));
        FileByteWriter test = App.get(ManagerFileByteWriter.class).get("default5", PersistentData.class);
        test.append(statisticSec1);
        test.flush(new AtomicBoolean(true));
    }

}