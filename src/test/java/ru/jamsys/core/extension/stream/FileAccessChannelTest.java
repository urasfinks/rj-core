package ru.jamsys.core.extension.stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.manager.item.log.LogType;
import ru.jamsys.core.component.manager.item.log.PersistentDataHeader;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilLog;

import java.math.BigDecimal;
import java.math.RoundingMode;


class FileAccessChannelTest {

    @Test
    void x0() throws Exception {
        UtilFile.removeAllFilesInFolder("LogManager");
        FileAccessChannel<PersistentDataHeader> fileAccessChannel = new FileAccessChannel<>(
                "LogManager/1.txt",
                UtilByte.megabytesToBytes(20),
                PersistentDataHeader.class
        );

        PersistentDataHeader persistentDataHeader = new PersistentDataHeader(LogType.INFO, FileAccessChannelTest.class, "Hello");
        persistentDataHeader.setWriterFlag((short) 4);

        fileAccessChannel.write(persistentDataHeader);
        fileAccessChannel.write(persistentDataHeader);

        fileAccessChannel.close();
        UtilLog.printInfo(FileAccessChannelTest.class, fileAccessChannel.getCopyQueue());
        Assertions.assertEquals(234, fileAccessChannel.getLength());
    }

    @Test
    void x1() throws Exception {
        UtilFile.removeAllFilesInFolder("LogManager");
        FileAccessChannel<PersistentDataHeader> fileAccessChannel = new FileAccessChannel<>(
                "LogManager/1.txt",
                UtilByte.megabytesToBytes(20),
                PersistentDataHeader.class
        );

        PersistentDataHeader persistentDataHeader = new PersistentDataHeader(LogType.INFO, FileAccessChannelTest.class, "Hello");
        persistentDataHeader.setWriterFlag((short) 4);

        fileAccessChannel.write(persistentDataHeader);
        // Ещё раз запишем
        fileAccessChannel.write(persistentDataHeader);

        Assertions.assertEquals(2, fileAccessChannel.getCopyQueue().size());
        Assertions.assertNotNull(fileAccessChannel.getCopyQueue().getFirst().getBytes());
        //UtilLog.printInfo(FileAccessChannelTest.class, fileAccessChannel.getCopyQueue());

        fileAccessChannel.close();

        fileAccessChannel = new FileAccessChannel<>(
                "LogManager/1.txt",
                UtilByte.megabytesToBytes(20),
                PersistentDataHeader.class
        );
        //Мы только что создали объект, он должен был подсосать данные из файла и сделать разметку
        Assertions.assertEquals(2, fileAccessChannel.getCopyQueue().size());
        Assertions.assertNull(fileAccessChannel.getCopyQueue().getFirst().getBytes());

        //Проливаем данные с ФС в объект
        FileAccessChannel.BlockInfo<PersistentDataHeader> first = fileAccessChannel.getCopyQueue().getFirst();
        fileAccessChannel.read(first);
        Assertions.assertNotNull(first.getBytes());

        UtilLog.printInfo(FileAccessChannelTest.class, fileAccessChannel.getCopyQueue());
        fileAccessChannel.close();
    }

    @Test
    void thread() throws Exception {
        UtilFile.removeAllFilesInFolder("LogManager");
        FileAccessChannel<PersistentDataHeader> fileAccessChannel = new FileAccessChannel<>(
                "LogManager/1.txt",
                UtilByte.megabytesToBytes(200),
                PersistentDataHeader.class
        );

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                String name = Thread.currentThread().getName();
                for (int j = 0; j < 100000; j++) {
                    try {
                        PersistentDataHeader persistentDataHeader = new PersistentDataHeader(
                                LogType.INFO,
                                FileAccessChannelTest.class,
                                name + " " + j
                        );
                        fileAccessChannel.write(persistentDataHeader);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            thread.start();
            thread.join();
        }

        long l = System.currentTimeMillis() - start;
        System.out.println("Time: " + l);

        BigDecimal transactionTime = new BigDecimal(l).divide(new BigDecimal(1000000), 5, RoundingMode.HALF_UP);
        BigDecimal tps = new BigDecimal(1000).divide(transactionTime, 5, RoundingMode.HALF_UP);
        System.out.println("Tps: " + tps);
        Assertions.assertEquals(1000000, fileAccessChannel.getCopyQueue().size());
    }
}