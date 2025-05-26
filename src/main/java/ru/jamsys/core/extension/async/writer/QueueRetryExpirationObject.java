package ru.jamsys.core.extension.async.writer;

import java.util.concurrent.ConcurrentLinkedDeque;

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
