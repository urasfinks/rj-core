package ru.jamsys.core.extension.raw.writer;

import lombok.Getter;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.flat.util.UtilByte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public class RawFileChannel<T extends ByteSerialization> {

    // Очередь для хранения блоков
    private final ConcurrentLinkedDeque<BlockInfo<T>> queue = new ConcurrentLinkedDeque<>();

    private final AtomicLong fileLength = new AtomicLong(0);

    private final Class<T> cls;

    private final RandomAccessFile file;

    private final FileChannel channel;

    @Getter
    private final String filePath;

    // Для быстрого доступа надо файл сразу аллоцировать по размеру
    // Если в процессе расширять - это трудоёмкая операция, для примера: с аллокацией 1_000_000 вставка - 2400мс
    // без аллокации вставка 1_000_000 - 3900мс, если быть грубым: почти в 2 раза
    public RawFileChannel(String filePath, long fileSizeAllocate, Class<T> cls) throws Exception {
        this.filePath = filePath;
        this.file = new RandomAccessFile(filePath, "rw");
        this.file.setLength(fileSizeAllocate);
        this.channel = this.file.getChannel();
        this.cls = cls;


        long currentPosition = 0; // Текущая позиция в файле
        // Читаем файл блоками
        long length = file.length();
        while (currentPosition < length) {
            file.seek(currentPosition);
            short writerFlag = file.readShort(); // Читаем 2 байта (short)
            int dataLength = file.readInt(); // Читаем 4 байта (int) - длина данных
            if (dataLength == 0) { // Если длина данных 0 - значит данные закончились
                break;
            }
            fileLength.addAndGet(6 + dataLength);
            // Создаем объект BlockInfo и добавляем его в очередь
            BlockInfo<T> blockInfo = new BlockInfo<>(
                    currentPosition,
                    writerFlag,
                    dataLength,
                    cls
            );
            queue.add(blockInfo);
            // Перемещаем указатель на следующий блок
            currentPosition += 6 + dataLength; // 6 = 2 (short) + 4 (int)
        }
    }

    public long getDataLength() {
        return fileLength.get();
    }

    public BlockInfo<T> write(ByteSerialization item) throws Exception {
        BlockInfo<T> blockInfo = allocate(item);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(UtilByte.shortToBytes(blockInfo.getWriterFlag()));
        os.write(UtilByte.intToBytes(blockInfo.getDataLength()));
        os.write(blockInfo.getBytes());

        int _ = channel.write(ByteBuffer.wrap(os.toByteArray()), blockInfo.getPosition());

        queue.add(blockInfo);
        return blockInfo;
    }

    public void read(BlockInfo<T> blockInfo) throws IOException {
        if (blockInfo == null) {
            return;
        }
        file.seek(blockInfo.getPosition() + 6);
        byte[] buffer = new byte[blockInfo.getDataLength()];
        int _ = file.read(buffer);
        blockInfo.setBytes(buffer);
    }

    private BlockInfo<T> allocate(ByteSerialization item) throws Exception {
        byte[] itemByte = item.toByte();
        int blockLength = 6 + itemByte.length;
        long position = fileLength.addAndGet(blockLength) - blockLength;
        return new BlockInfo<>(
                position,
                item.getWriterFlag(),
                itemByte.length,
                cls
        )
                .setBytes(itemByte);
    }

    public BlockInfo<T> pollFirst() {
        return queue.pollFirst();
    }

    public BlockInfo<T> pollLast() {
        return queue.pollLast();
    }

    public List<BlockInfo<T>> getCopyQueue() {
        return new ArrayList<>(queue);
    }

    public void close() throws IOException {
        channel.close();
        file.close();
    }

}
