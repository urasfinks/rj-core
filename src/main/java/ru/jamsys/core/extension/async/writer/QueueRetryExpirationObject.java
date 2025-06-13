package ru.jamsys.core.extension.async.writer;

import java.util.concurrent.ConcurrentLinkedDeque;

// Для использования общей ExpirationList в разрезе всех QueueRetry был создан такой объект, который хранит ссылку на
// очередь, куда вставлять данные по наступлению timeout

public class QueueRetryExpirationObject {

    private final ConcurrentLinkedDeque<DataReadWrite> park;

    private final DataReadWrite dataReadWrite;

    public QueueRetryExpirationObject(ConcurrentLinkedDeque<DataReadWrite> park, DataReadWrite dataReadWrite) {
        this.park = park;
        this.dataReadWrite = dataReadWrite;
    }

    public void insert() {
        park.add(dataReadWrite);
    }

}
