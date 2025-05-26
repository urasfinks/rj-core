package ru.jamsys.core.extension.async.writer;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.expiration.ExpirationList;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

// Очередь данных, которые можно взять на обработку на какое-то время. Если за это время не выполнить remove, данные
// снова вернуться в очередь и будут повторно выданы

public class QueueRetry implements DataReader, StatisticsFlush {

    @Getter
    @Setter
    private volatile boolean error = false; // Ошибка чтения данных

    @Getter
    @Setter
    private volatile boolean finishState; // Встретили -1 длину данных. -1 это EOF

    private final ConcurrentLinkedDeque<DataReadWrite> park = new ConcurrentLinkedDeque<>();

    private final ConcurrentHashMap<Long, DataReadWrite> position = new ConcurrentHashMap<>(); // key: position;

    private final String key;

    @Getter
    private final ManagerConfiguration<ExpirationList<QueueRetryExpirationObject>> expirationListConfiguration;

    public QueueRetry(String key, boolean finishState) {
        this.key = key;
        this.finishState = finishState;
        expirationListConfiguration = ManagerConfiguration.getInstance(
                ExpirationList.class,
                QueueRetry.class.getName(), // Это общий ExpirationList для всех экземпляров QueueRetry
                queueRetryExpirationObjectExpirationList -> queueRetryExpirationObjectExpirationList
                        .setupOnExpired(QueueRetryExpirationObject::insert)
        );
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("key", key)
                .append("finishState", finishState)
                .append("parkSize", park.size())
                .append("positionSize", position.size())
                ;
    }

    // Размер не обработанных элементов
    public int size() {
        return position.size();
    }

    @Override
    public void add(@NonNull DataReadWrite dataReadWrite) {
        // Хотел через computeIfAbsent, основа которого не вызывать исполнение лямды в случае отсутствия позиции,
        // но тогда надо делать атомики для контроля добавления и это больше исключение чем постоянная история
        if (position.putIfAbsent(dataReadWrite.getPosition(), dataReadWrite) == null) {
            park.add(dataReadWrite);
        } else {
            App.error(new RuntimeException("Duplicate DataReadWrite detected at position: " + dataReadWrite.getPosition()));
        }
    }

    public void remove(long position) {
        DataReadWrite dataReadWrite = this.position.remove(position);
        if (dataReadWrite != null) {
            dataReadWrite.setRemove(true);
            DisposableExpirationMsImmutableEnvelope<?> expiration = dataReadWrite.getExpiration();
            if (expiration != null) {
                // Нейтрализуем что бы expirationList не взял его в обработку при timeout
                if (!expiration.doNeutralized()) {
                    App.error(new RuntimeException("Unfortunately, the block has already entered retry"));
                }
            }
        }
    }

    public DataReadWrite pollLast(long timeoutMs) {
        return pollLast(timeoutMs, System.currentTimeMillis());
    }

    public DataReadWrite pollLast(long timeoutMs, long currentTimestamp) {
        DataReadWrite dataReadWrite;
        while (true) {
            dataReadWrite = park.pollLast();
            if (dataReadWrite == null) {
                break;
            }
            if (!dataReadWrite.isRemove()) {
                QueueRetryExpirationObject foreignExpirationObject = new QueueRetryExpirationObject(
                        park,
                        dataReadWrite
                );
                dataReadWrite.setExpiration(expirationListConfiguration
                        .get()
                        .add(new ExpirationMsImmutableEnvelope<>(foreignExpirationObject, timeoutMs, currentTimestamp))
                );
                return dataReadWrite;
            }
        }
        return null;
    }

    // Очередь полностью обработана
    public boolean isProcessed() {
        return finishState && position.isEmpty();
    }

    // Только для unit тестов
    public DataReadWrite getForUnitTest(long position) {
        return this.position.get(position);
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of(new DataHeader()
                .setBody(key)
                .addHeader("size", position.size())
                .addHeader("size", position.size())
        );
    }

}
