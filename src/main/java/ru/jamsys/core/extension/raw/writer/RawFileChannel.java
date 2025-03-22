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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// Класс сырой записи в файл в многопоточном режиме
// Можно записать свой собственный ByteSerialization только в конец файла
// При инициализации происходит чтение разметки данных из файла - RawFileBlock
// который содержит начало блока, StatusCode и размер блока.
// Блоки помещаются в очередь, которую можно вычитать и сделать в дальнейшем правки в StatusCode (commit)
// Для чего:
// Вот у нас есть файл с логами, логи надо доставить до удалённого хранилища
// Как быть и как проконтролировать что всё было доставлено на удалённый сервер?
// 1) Считать весь файл в память и тихонечко грузить на сервер, но если всё упадёт мы начнём весь процесс с начала
// 2) После считывания - файл удаляем
// А что если не получилось доставить какой-то лог? И тут начинается перекладывание, с пометками
// В целом я наперекладывался так, что это стало вообще не управляемым и сложным.
// Начал думать в сторону транзакций, есть 2 принципа работы:
// 1) Дупликация - когда мы ждём commit, что операция выполнилась. Если что-то произошло и мы не
//    получили статус - у нас будет повтор
// 2) Потеря данных, когда происходит auto commit - у нас данные забрали - всё. Мы больше эти данные не отдадим никому
// Всё остальное - это митигация архитектурой, которая не позволит сделать дубль или потерять данные
// Например вкладывать в данные идентификатор будущего запроса, и если операция где-то сломается, другая сторона должна
// проверять дубли в случае повтора операции.

public class RawFileChannel<T extends ByteSerialization> implements Closable, ResourceQueue<RawFileBlock<T>> {

    // Очередь для хранения блоков
    private final ConcurrentLinkedDeque<RawFileBlock<T>> queue = new ConcurrentLinkedDeque<>();

    private final AtomicInteger queueSize = new AtomicInteger(0);

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
            short statusCode = file.readShort(); // Читаем 2 байта (short)
            int dataLength = file.readInt(); // Читаем 4 байта (int) - длина данных
            if (dataLength == 0) { // Если длина данных 0 - значит данные закончились
                break;
            }
            fileLength.addAndGet(6 + dataLength);
            // Создаем объект BlockInfo и добавляем его в очередь
            RawFileBlock<T> rawFileBlock = new RawFileBlock<>(
                    currentPosition,
                    statusCode,
                    dataLength,
                    cls
            );
            queue.add(rawFileBlock);
            queueSize.incrementAndGet();
            // Перемещаем указатель на следующий блок
            currentPosition += 6 + dataLength; // 6 = 2 (short) + 4 (int)
        }
    }

    public List<RawFileBlock<T>> getCopyQueue() {
        return new ArrayList<>(queue);
    }

    public void updateStatusCode(RawFileBlock<T> rawFileBlock) throws Exception {
        int _ = channel.write(
                ByteBuffer.wrap(UtilByte.shortToBytes(rawFileBlock.getStateCode())),
                rawFileBlock.getPosition()
        );
    }

    public void read(RawFileBlock<T> rawFileBlock) throws IOException {
        if (rawFileBlock == null) {
            return;
        }
        file.seek(rawFileBlock.getPosition() + 6);
        byte[] buffer = new byte[rawFileBlock.getDataLength()];
        int _ = file.read(buffer);
        rawFileBlock.setBytes(buffer);
    }

    public RawFileBlock<T> convert(T item) throws Exception {
        byte[] itemByte = item.toByte();
        int blockLength = 6 + itemByte.length;
        long position = fileLength.addAndGet(blockLength) - blockLength;
        return new RawFileBlock<>(
                position,
                item.getStatusCode(),
                itemByte.length,
                cls
        )
                .setBytes(itemByte);
    }

    @Override
    public long size() {
        return queueSize.get();
    }

    public long sizeByte() {
        return fileLength.get();
    }

    @Override
    public void add(RawFileBlock<T> rawFileBlock) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(UtilByte.shortToBytes(rawFileBlock.getStateCode()));
        os.write(UtilByte.intToBytes(rawFileBlock.getDataLength()));
        os.write(rawFileBlock.getBytes());

        int _ = channel.write(ByteBuffer.wrap(os.toByteArray()), rawFileBlock.getPosition());

        queue.add(rawFileBlock);
        queueSize.incrementAndGet();
    }

    public RawFileBlock<T> pollFirst() {
        RawFileBlock<T> tRawFileBlock = queue.pollFirst();
        if (tRawFileBlock != null) {
            queueSize.decrementAndGet();
        }
        return tRawFileBlock;
    }

    public RawFileBlock<T> pollLast() {
        RawFileBlock<T> tRawFileBlock = queue.pollLast();
        if (tRawFileBlock != null) {
            queueSize.decrementAndGet();
        }
        return tRawFileBlock;
    }

    public void close() throws Exception {
        channel.close();
        file.close();
    }

}
