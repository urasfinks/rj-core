package ru.jamsys.core.extension.raw.writer;

import com.google.common.base.Function;
import lombok.Getter;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.flat.util.UtilByte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// Класс сырой записи в файл в многопоточном режиме
// Можно записать свой собственный ByteSerialization только в конец файла
// При инициализации происходит чтение разметки данных из файла - RawFileBlock
// который содержит начало блока, subscriberStatusRead и размер блока.
// Блоки помещаются в очередь, которую можно вычитать и сделать в дальнейшем правки в subscriberStatusRead (commit)
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

public class RawFileChannel<T extends ByteSerialization>
        implements
        Closable,
        ResourceQueue<RawFileMarkup<T>> {

    // Очередь для хранения блоков
    private final ConcurrentLinkedDeque<RawFileMarkup<T>> queue = new ConcurrentLinkedDeque<>();

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
            short subscriberStatusRead = file.readShort(); // Читаем 2 байта (short)
            int dataLength = file.readInt(); // Читаем 4 байта (int) - длина данных
            if (dataLength == 0) { // Если длина данных 0 - значит данные закончились
                break;
            }
            fileLength.addAndGet(6 + dataLength);
            // Создаем объект BlockInfo и добавляем его в очередь
            RawFileMarkup<T> rawFileMarkup = new RawFileMarkup<>(
                    currentPosition,
                    subscriberStatusRead,
                    dataLength,
                    cls
            );
            queue.add(rawFileMarkup);
            queueSize.incrementAndGet();
            // Перемещаем указатель на следующий блок
            currentPosition += 6 + dataLength; // 6 = 2 (short) + 4 (int)
        }
    }

    public List<RawFileMarkup<T>> getCopyQueue() {
        return new ArrayList<>(queue);
    }

    public void updateSubscriberStatusRead(RawFileMarkup<T> rawFileMarkup) throws Exception {
        int _ = channel.write(
                ByteBuffer.wrap(UtilByte.shortToBytes(rawFileMarkup.getStateCode())),
                rawFileMarkup.getPosition()
        );
    }

    // Синхронизованное обновление SubscriberStatusRead если предполагается конкуренция
    public void updateSubscriberStatusRead(
            RawFileMarkup<T> rawFileMarkup,
            Function<Short, Short> getNewSubscriberStatusRead
    ) throws Exception {
        try (FileLock _ = channel.lock(rawFileMarkup.getPosition(), 2, false)) {
            // Считаем актуальные SubscriberStatusRead
            ByteBuffer buffer = ByteBuffer.allocate(2);
            channel.read(buffer, rawFileMarkup.getPosition());
            short currentSubscriberStatusRead = UtilByte.bytesToShort(buffer.array());
            int _ = channel.write(
                    ByteBuffer.wrap(UtilByte.shortToBytes(getNewSubscriberStatusRead.apply(currentSubscriberStatusRead))),
                    rawFileMarkup.getPosition()
            );
        }
    }

    public void read(RawFileMarkup<T> rawFileMarkup) throws IOException {
        if (rawFileMarkup == null) {
            return;
        }
        try (FileLock _ = channel.lock(rawFileMarkup.getPosition() + 6, rawFileMarkup.getDataLength(), true)) {
            ByteBuffer buffer = ByteBuffer.allocate(rawFileMarkup.getDataLength());
            channel.read(buffer, rawFileMarkup.getPosition() + 6);
            rawFileMarkup.setBytes(buffer.array());
        }
    }

    public RawFileMarkup<T> convert(T item) throws Exception {
        byte[] itemByte = item.toByte();
        int blockLength = 6 + itemByte.length;
        long position = fileLength.addAndGet(blockLength) - blockLength;
        return new RawFileMarkup<>(
                position,
                item.getSubscriberStatusRead(),
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
    public void add(RawFileMarkup<T> rawFileMarkup) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(UtilByte.shortToBytes(rawFileMarkup.getStateCode()));
        os.write(UtilByte.intToBytes(rawFileMarkup.getDataLength()));
        os.write(rawFileMarkup.getBytes());

        int _ = channel.write(ByteBuffer.wrap(os.toByteArray()), rawFileMarkup.getPosition());

        queue.add(rawFileMarkup);
        queueSize.incrementAndGet();
    }

    public RawFileMarkup<T> pollFirst() {
        RawFileMarkup<T> tRawFileMarkup = queue.pollFirst();
        if (tRawFileMarkup != null) {
            queueSize.decrementAndGet();
        }
        return tRawFileMarkup;
    }

    public RawFileMarkup<T> pollLast() {
        RawFileMarkup<T> tRawFileMarkup = queue.pollLast();
        if (tRawFileMarkup != null) {
            queueSize.decrementAndGet();
        }
        return tRawFileMarkup;
    }

    public void close() throws Exception {
        channel.close();
        file.close();
    }

}
