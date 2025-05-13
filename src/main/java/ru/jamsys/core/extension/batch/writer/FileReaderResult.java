package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Сегментированные по позиции чтения данные из файла
@Getter
public class FileReaderResult implements DataReadable {

    private final ConcurrentHashMap<Long, DataPayload> mapData = new ConcurrentHashMap<>(); // key: position;

    private final AtomicInteger size = new AtomicInteger(0); // Счётчик оставшихся позиций

    @Setter
    private volatile boolean error = false; // Ошибка чтения данных

    @Setter
    private volatile boolean finishState = false; // Встретили -1 длину данных в bin

    @Override
    public void add(DataPayload dataPayload) {
        mapData.computeIfAbsent(dataPayload.getPosition(), _ -> {
            size.incrementAndGet();
            return dataPayload;
        });
    }

    public void remove(long position) {
        if (mapData.remove(position) != null) {
            size.decrementAndGet();
        }
    }

    public int size() {
        return size.get();
    }

}
