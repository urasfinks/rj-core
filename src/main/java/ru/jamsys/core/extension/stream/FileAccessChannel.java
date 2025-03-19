package ru.jamsys.core.extension.stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilByte;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public class FileAccessChannel<T extends ByteSerialization> {

    @Getter
    @Accessors(chain = true)
    public static class BlockInfo<TX extends ByteSerialization> {

        @Setter
        private short writerFlag; //

        private final long position; // Позиция начала блока

        private final int length;   // Длина данных

        @JsonIgnore
        private byte[] bytes;

        private final Class<TX> cls;

        BlockInfo(short writerFlag, long position, int length, Class<TX> cls) {
            this.writerFlag = writerFlag;
            this.position = position;
            this.length = length;
            this.cls = cls;
        }

        public BlockInfo<TX> setBytes(byte[] bytes) {
            if (bytes.length != length) {
                throw new RuntimeException("allocation byte size != current byte size");
            }
            this.bytes = bytes;

            return this;
        }

        @JsonProperty("data")
        public TX cast() throws Exception {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            TX item = cls.getConstructor().newInstance();
            item.toObject(bytes);
            item.setWriterFlag(writerFlag);
            return item;
        }

    }

    private final FileChannel channel;

    private final RandomAccessFile file;

    // Что бы не было такого, что с боку будут подсовывать не наши размеченные данные.
    // Мы сами являемся создателями BlockInfo.
    private final Set<BlockInfo<T>> reg = Util.getConcurrentHashSet();

    // Очередь для хранения блоков
    private final ConcurrentLinkedDeque<BlockInfo<T>> queue = new ConcurrentLinkedDeque<>();

    private final AtomicLong fileLength = new AtomicLong(0);

    private final Class<T> cls;

    public FileAccessChannel(String filePath, Class<T> cls) throws Exception {
        this.file = new RandomAccessFile(filePath, "rw");
        this.channel = file.getChannel();
        this.fileLength.set(file.length()); // Получаем длину файла
        this.cls = cls;
        init(false);
    }

    public FileAccessChannel(String filePath, Class<T> cls, boolean loadDataOnInit) throws Exception {
        this.file = new RandomAccessFile(filePath, "rw");
        this.channel = file.getChannel();
        this.fileLength.set(file.length()); // Получаем длину файла
        this.cls = cls;
        init(loadDataOnInit);
    }

    private void init(boolean loadDataOnInit) throws Exception {
        long currentPosition = 0; // Текущая позиция в файле
        // Читаем файл блоками
        while (currentPosition < this.fileLength.get()) {
            file.seek(currentPosition);
            short writerFlag = file.readShort(); // Читаем 2 байта (short)
            int dataLength = file.readInt(); // Читаем 4 байта (int) - длина данных
            // Создаем объект BlockInfo и добавляем его в очередь
            BlockInfo<T> blockInfo = new BlockInfo<>(writerFlag, currentPosition, dataLength, cls);
            // Если надо читать, ну читаем, но не от всего сердца (smile).
            // Как будто потоки должны сами читать, что бы память не забивать приложения
            if (loadDataOnInit) {
                byte[] buffer = new byte[dataLength];
                int _ = file.read(buffer);
                blockInfo.setBytes(buffer);
            }
            reg.add(blockInfo);
            queue.add(blockInfo);
            // Перемещаем указатель на следующий блок
            currentPosition += 6 + dataLength; // 6 = 2 (short) + 4 (int)
        }
    }

    public void writeFlag(BlockInfo<T> blockInfo, short flag) throws IOException {
        if (blockInfo == null) {
            return;
        }
        try (FileLock _ = channel.lock(blockInfo.getPosition(), 2, false)) {
            ByteBuffer buffer = ByteBuffer.wrap(UtilByte.shortToBytes(flag));
            int _ = channel.write(buffer, blockInfo.getPosition());
            blockInfo.setWriterFlag(flag);
        }
    }

    public BlockInfo<T> write(ByteSerialization item) throws Exception {
        if (item == null) {
            return null;
        }
        BlockInfo<T> blockInfo = allocate(item);
        try (FileLock _ = channel.lock(blockInfo.getPosition(), 6 + blockInfo.getLength(), false)) {
            file.writeShort(blockInfo.getWriterFlag());
            file.writeInt(blockInfo.getLength());
            file.write(blockInfo.getBytes());

            reg.add(blockInfo);
            queue.add(blockInfo);
        }
        return blockInfo;
    }

    public void read(BlockInfo<T> blockInfo) throws IOException {
        if (blockInfo == null) {
            return;
        }
        if (!reg.contains(blockInfo)) {
            throw new RuntimeException("blockInfo not contains in reg");
        }
        try (FileLock _ = channel.lock(blockInfo.getPosition() + 2, blockInfo.getLength(), true)) {
            ByteBuffer buffer = ByteBuffer.allocate(blockInfo.getLength());
            channel.read(buffer, blockInfo.getPosition() + 6);
            blockInfo.setBytes(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws IOException {
        channel.close();
        file.close();
    }

    private BlockInfo<T> allocate(ByteSerialization item) throws Exception {
        byte[] itemByte = item.toByte();
        long itemBodyLength = 6 + itemByte.length;
        long newEof = fileLength.addAndGet(itemBodyLength);
        return new BlockInfo<>(
                item.getWriterFlag(),
                newEof - itemBodyLength,
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

}
