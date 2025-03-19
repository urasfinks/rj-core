package ru.jamsys.core.extension.stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.manager.item.log.PersistentDataHeader;
import ru.jamsys.core.component.manager.item.log.LogType;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilLog;

class FileAccessChannelTest {
    @Test
    void x1() throws Exception {
        UtilFile.removeAllFilesInFolder("LogManager");
        FileAccessChannel<PersistentDataHeader> fileAccessChannel = new FileAccessChannel<>("LogManager/1.txt", PersistentDataHeader.class);

        PersistentDataHeader persistentDataHeader = new PersistentDataHeader(LogType.INFO, FileAccessChannelTest.class, "Hello");
        persistentDataHeader.setWriterFlag((short) 4);

        fileAccessChannel.write(persistentDataHeader);
        // Ещё раз запишем
        fileAccessChannel.write(persistentDataHeader);

        Assertions.assertEquals(2, fileAccessChannel.getCopyQueue().size());
        Assertions.assertNotNull(fileAccessChannel.getCopyQueue().getFirst().getBytes());
        //UtilLog.printInfo(FileAccessChannelTest.class, fileAccessChannel.getCopyQueue());

        fileAccessChannel.close();

        fileAccessChannel = new FileAccessChannel<>("LogManager/1.txt", PersistentDataHeader.class);
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
}