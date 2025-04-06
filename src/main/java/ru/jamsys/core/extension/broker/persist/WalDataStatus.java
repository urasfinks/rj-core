package ru.jamsys.core.extension.broker.persist;

import lombok.Setter;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.flat.util.UtilRisc;

import java.nio.ByteBuffer;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

// [ operation = [insert_data|insert_group|pool|commit] | id | timestamp |  idGroup ]


public class WalDataStatus extends AbstractLifeCycle implements LifeCycleInterface {

    public enum Operation {
        SUBSCRIBE_GROUP((byte) 1), //Подписана группа
        INSERT_DATA((byte) 2), // Добавлены данные
        POLL_DATA_GROUP((byte) 3), // Данные были выданы для группы
        COMMIT_DATA_GROUP((byte) 4), // Данные были обработаны группой
        UNSUBSCRIBE_GROUP((byte) 5), // Группа отписана
        ;

        private final byte operation;

        Operation(byte operation) {
            this.operation = operation;
        }

        public byte getByte() {
            return operation;
        }

    }

    @Setter
    private volatile long timeRetryMs = 60_000;

    private BatchFileWriter batchFileWriter;

    private final String filePath;

    public WalDataStatus(String filePath) {
        this.filePath = filePath;
    }

    private final ConcurrentHashMap<
            Byte, //idGroup
            AtomicBoolean //lock
            > groupLock = new ConcurrentHashMap<>();

    ConcurrentHashMap<
            Byte, //idGroup
            ConcurrentSkipListMap<
                    Long, //idData
                    Long //timestamp
                    >
            > groupMap = new ConcurrentHashMap<>();

    public void subscribeGroup(byte idGroup, long timestamp) {
        groupLock.computeIfAbsent(idGroup, _ -> new AtomicBoolean(false));
        groupMap.computeIfAbsent(idGroup, _ -> {
            createRecord(
                    timestamp,
                    Operation.SUBSCRIBE_GROUP.getByte(),
                    0,
                    idGroup
            );
            return new ConcurrentSkipListMap<>();
        });
    }

    public void unsubscribeGroup(byte idGroup, long timestamp) {
        createRecord(
                timestamp,
                Operation.UNSUBSCRIBE_GROUP.getByte(),
                0,
                idGroup
        );
        groupLock.remove(idGroup);
        groupMap.remove(idGroup);
    }

    public void addData(long idData, long timestamp) {
        UtilRisc.forEach(null, groupMap, (idGroup, longLongConcurrentSkipListMap) -> {
            createRecord(
                    timestamp,
                    Operation.INSERT_DATA.getByte(),
                    idData,
                    idGroup
            );
            longLongConcurrentSkipListMap.put(idData, timestamp);
        });
    }

    private byte[] createRecord(long timestamp, byte operation, long idData, byte idGroup) {
        return ByteBuffer.allocate(18)
                .putLong(timestamp)
                .put(operation)
                .putLong(idData)
                .put(idGroup)
                .flip()
                .array();
    }

    public List<Long> getFirst(byte idGroup, int size) {
        return getFirst(idGroup, size,  System.currentTimeMillis());
    }

    public List<Long> getFirst(byte idGroup, int size, long now) {
        List<Long> result = new ArrayList<>();
        AtomicBoolean lock = groupLock.computeIfAbsent(idGroup, _ -> new AtomicBoolean(false));
        if (lock.compareAndSet(false, true)) {
            try {
                ConcurrentSkipListMap<Long, Long> groupDataMap = groupMap.get(idGroup);
                if (groupDataMap == null || groupDataMap.isEmpty()) {
                    return result;
                }
                Map.Entry<Long, Long> idEntity = groupDataMap.firstEntry();
                while (size > 0 && idEntity != null) {
                    Long timestamp = idEntity.getValue();
                    Long id = idEntity.getKey();
                    if ((now >= timestamp + timeRetryMs) && groupDataMap.replace(id, timestamp, now)) {
                        result.add(id);
                        size--;
                    }
                    idEntity = groupDataMap.higherEntry(idEntity.getKey());
                }
            } finally {
                lock.set(false);
            }
        }
        return result;
    }

    public List<Long> getLast(byte idGroup, int size) {
        return getLast(idGroup, size,  System.currentTimeMillis());
    }

    public List<Long> getLast(byte idGroup, int size, long now) {
        List<Long> result = new ArrayList<>();
        AtomicBoolean lock = groupLock.computeIfAbsent(idGroup, _ -> new AtomicBoolean(false));
        if (lock.compareAndSet(false, true)) {
            try {
                ConcurrentSkipListMap<Long, Long> groupDataMap = groupMap.get(idGroup);
                if (groupDataMap == null || groupDataMap.isEmpty()) {
                    return result;
                }
                Map.Entry<Long, Long> idEntity = groupDataMap.lastEntry();
                while (size > 0 && idEntity != null) {
                    Long timestamp = idEntity.getValue();
                    Long id = idEntity.getKey();
                    if ((now >= timestamp + timeRetryMs) && groupDataMap.replace(id, timestamp, now)) {
                        result.add(id);
                        size--;
                    }
                    idEntity = groupDataMap.lowerEntry(idEntity.getKey());
                }
            } finally {
                lock.set(false);
            }
        }
        return result;
    }

    @Override
    public void runOperation() {
        batchFileWriter = new BatchFileWriter(filePath);
        batchFileWriter.setOpenOption(StandardOpenOption.APPEND);
        batchFileWriter.run();
    }

    @Override
    public void shutdownOperation() {
        if (batchFileWriter != null) {
            groupMap.clear();
            groupLock.clear();
            batchFileWriter.shutdown();
        }
    }

}
