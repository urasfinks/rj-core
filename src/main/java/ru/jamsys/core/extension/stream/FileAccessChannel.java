package ru.jamsys.core.extension.stream;

import lombok.Getter;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.flat.util.UtilByte;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public class FileAccessChannel {

    @Getter
    public static class BlockInfo {
        private final short writerFlag; //
        private final long position; // Позиция начала блока
        private final int length;   // Длина данных

        BlockInfo(short writerFlag, long position, int length) {
            this.writerFlag = writerFlag;
            this.position = position;
            this.length = length;
        }
    }

    private final FileChannel channel;
    private final RandomAccessFile file;
    private final ConcurrentLinkedDeque<BlockInfo> queue = new ConcurrentLinkedDeque<>(); // Очередь для хранения блоков
    private final AtomicLong fileLength = new AtomicLong(0);

    public FileAccessChannel(String filePath) throws IOException {
        this.file = new RandomAccessFile(filePath, "rw");
        this.channel = file.getChannel();
        this.fileLength.set(file.length()); // Получаем длину файла

        long currentPosition = 0; // Текущая позиция в файле
        // Читаем файл блоками
        while (currentPosition < this.fileLength.get()) {
            file.seek(currentPosition);
            short writerFlag = file.readShort(); // Читаем 2 байта (short)
            int dataLength = file.readInt(); // Читаем 4 байта (int) - длина данных
            // Создаем объект BlockInfo и добавляем его в очередь
            queue.add(new BlockInfo(writerFlag, currentPosition, dataLength));
            // Перемещаем указатель на следующий блок
            currentPosition += 6 + dataLength; // 6 = 2 (short) + 4 (int)
        }
    }

    public void writeFlag(BlockInfo blockInfo, short flag) throws IOException {
        try (FileLock _ = channel.lock(blockInfo.getPosition(), 2, false)) {
            ByteBuffer buffer = ByteBuffer.wrap(UtilByte.shortToBytes(flag));
            int _ = channel.write(buffer, blockInfo.getPosition());
        }
    }

    public void writeTail(ByteSerialization item) throws Exception {
        byte[] itemByte = item.toByte();
        try (FileLock _ = channel.lock(fileLength.get(), 2 + itemByte.length, false)) {
            int _ = channel.write(ByteBuffer.wrap(UtilByte.shortToBytes(item.getWriterFlag())), 2);
            int _ = channel.write(ByteBuffer.wrap(itemByte), fileLength.get());
        }
    }

    public byte[] read(BlockInfo blockInfo) throws IOException {
        try (FileLock _ = channel.lock(position, length, true)) {
            ByteBuffer buffer = ByteBuffer.allocate(length);
            channel.read(buffer, position);
            return buffer.array();
        }
    }

    public void close() throws IOException {
        channel.close();
        file.close();
    }

}
