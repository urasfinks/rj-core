package ru.jamsys.core.extension.stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.manager.item.log.LogType;
import ru.jamsys.core.component.manager.item.log.PersistentDataHeader;
import ru.jamsys.core.extension.raw.writer.BlockInfo;
import ru.jamsys.core.extension.raw.writer.RawFileChannel;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilLog;

import java.math.BigDecimal;
import java.math.RoundingMode;


class RawFileChannelTest {

    @Test
    void x0() throws Exception {
        UtilFile.removeAllFilesInFolder("LogManager");
        RawFileChannel<PersistentDataHeader> rawFileChannel = new RawFileChannel<>(
                "LogManager/1.txt",
                UtilByte.megabytesToBytes(20),
                PersistentDataHeader.class
        );

        PersistentDataHeader persistentDataHeader = new PersistentDataHeader(LogType.INFO, RawFileChannelTest.class, "Hello");
        persistentDataHeader.setWriterFlag((short) 4);

        rawFileChannel.write(persistentDataHeader);
        rawFileChannel.write(persistentDataHeader);

        rawFileChannel.close();
        UtilLog.printInfo(RawFileChannelTest.class, rawFileChannel.getCopyQueue());
        Assertions.assertEquals(228, rawFileChannel.getDataLength());
    }

    @Test
    void x1() throws Exception {
        UtilFile.removeAllFilesInFolder("LogManager");
        RawFileChannel<PersistentDataHeader> rawFileChannel = new RawFileChannel<>(
                "LogManager/1.txt",
                UtilByte.megabytesToBytes(20),
                PersistentDataHeader.class
        );

        PersistentDataHeader persistentDataHeader = new PersistentDataHeader(LogType.INFO, RawFileChannelTest.class, "Hello");
        persistentDataHeader.setWriterFlag((short) 4);

        rawFileChannel.write(persistentDataHeader);
        // Ещё раз запишем
        rawFileChannel.write(persistentDataHeader);

        Assertions.assertEquals(2, rawFileChannel.getCopyQueue().size());
        Assertions.assertNotNull(rawFileChannel.getCopyQueue().getFirst().getBytes());
        //UtilLog.printInfo(FileAccessChannelTest.class, fileAccessChannel.getCopyQueue());

        rawFileChannel.close();

        rawFileChannel = new RawFileChannel<>(
                "LogManager/1.txt",
                UtilByte.megabytesToBytes(20),
                PersistentDataHeader.class
        );
        //Мы только что создали объект, он должен был подсосать данные из файла и сделать разметку
        Assertions.assertEquals(2, rawFileChannel.getCopyQueue().size());
        Assertions.assertNull(rawFileChannel.getCopyQueue().getFirst().getBytes());

        //Проливаем данные с ФС в объект
        BlockInfo<PersistentDataHeader> first = rawFileChannel.getCopyQueue().getFirst();
        rawFileChannel.read(first);
        Assertions.assertNotNull(first.getBytes());

        UtilLog.printInfo(RawFileChannelTest.class, rawFileChannel.getCopyQueue());
        rawFileChannel.close();
    }

    @Test
    void thread() throws Exception {
        UtilFile.removeAllFilesInFolder("LogManager");
        RawFileChannel<PersistentDataHeader> rawFileChannel = new RawFileChannel<>(
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
                                RawFileChannelTest.class,
                                name + " " + j
                        );
                        rawFileChannel.write(persistentDataHeader);
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
        Assertions.assertEquals(1000000, rawFileChannel.getCopyQueue().size());
    }
}