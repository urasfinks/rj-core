package ru.jamsys.core.extension.broker.persist;

import lombok.Setter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

// Запись WAL файла, многопоточная запись статусов
public class WalFileWriter implements AutoCloseable {

    private final RandomAccessFile file;

    private final FileChannel channel;

    protected final AtomicLong dataOffset = new AtomicLong(0); // Смещение данных

    public static final int RECORD_SIZE = 10; // 8 (id) + 2 (idGroup) // размер записи WAL в байтах

    // Блокировка на расширение, что бы только 1 поток мог расширять файл
    private final AtomicBoolean lock = new AtomicBoolean(false);

    private final AtomicLong fileLength = new AtomicLong(0); // Текущий размер на файловой системе

    @Setter
    private long capacity; // Размер аллокации WAL файла сразу на файловой системе



    public WalFileWriter(String filePath, long capacity) throws IOException {
        this.file = new RandomAccessFile(filePath, "rwd");
        this.capacity = capacity;
        extendFileLength();
        this.channel = this.file.getChannel();
    }

    // Расширить размер файла, сделано для того, что бы каждая запись не двигала размер
    private void extendFileLength() throws IOException {
        this.file.setLength(fileLength.addAndGet(capacity));
    }

    // Не хочется делать write на вставке данных в .log файл (idGroup = 0 - это фиксация, что существуют такие данные)
    // Но тут необходимо как-то узнать
    public void write(long id, short idGroup) throws IOException {
        //walDataStatus.unsubscribe(id, idGroup);
        long offset = dataOffset.getAndAdd(RECORD_SIZE);
        if (offset > fileLength.get()) {
            if (lock.compareAndSet(false, true)) {
                try {
                    extendFileLength();
                } finally {
                    lock.set(false);
                }
            }
        }
        ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE)
                .putLong(id)
                .putShort(idGroup)
                .flip();

        try (FileLock fileLock = channel.lock(offset, RECORD_SIZE, false)) {
            while (buffer.hasRemaining()) {
                int written = fileLock.channel().write(buffer);
                if (written < 0) {
                    throw new IOException("Failed to write record");
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        try {
            channel.close();
        } finally {
            file.close();
        }
    }

}
