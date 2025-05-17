package ru.jamsys.core.extension.async.writer;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.expiration.ExpirationList;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

// Очередь данных, которые можно взять на обработку на какое-то время. Если за это время не выполнить remove, данные
// снова вернуться в очередь и будут повторно выданы

public class QueueRetry implements DataFromFile, StatisticsFlush {

    @Getter
    @Setter
    private volatile boolean error = false; // Ошибка чтения данных

    @Getter
    @Setter
    private volatile boolean finishState; // Встретили -1 длину данных в bin

    private final ConcurrentLinkedDeque<DataPayload> park = new ConcurrentLinkedDeque<>();

    private final ConcurrentHashMap<Long, DataPayload> position = new ConcurrentHashMap<>(); // key: position;

    @Getter
    private final Manager.Configuration<ExpirationList<DataPayload>> expirationListConfiguration;

    private final Map<DataPayload, DisposableExpirationMsImmutableEnvelope<DataPayload>> mapExpiration = new ConcurrentHashMap<>();

    public QueueRetry(String key, boolean finishState) {
        this.finishState = finishState;
        expirationListConfiguration = App.get(Manager.class).configureGeneric(
                ExpirationList.class,
                key,
                key1 -> new ExpirationList<>(
                        key1,
                        exp -> {
                            // Position не трогаем, там эти данные есть. Из position удаляется только тогда, когда
                            // происходит коммит X->Y->remove()
                            park.add(exp.getValue());
                        }
                )
        );
    }

    // Размер не обработанных элементов
    public int size() {
        return position.size();
    }

    public boolean parkIsEmpty() {
        return park.isEmpty();
    }

    @Override
    public void add(@NonNull DataPayload dataPayload) {
        position.computeIfAbsent(dataPayload.getPosition(), _ -> {
            park.add(dataPayload);
            return dataPayload;
        });
    }

    public void remove(long position) {
        DataPayload dataPayload = this.position.remove(position);
        if (dataPayload != null) {
            park.remove(dataPayload);
            DisposableExpirationMsImmutableEnvelope<DataPayload> remove = mapExpiration.remove(dataPayload);
            if (remove != null && expirationListConfiguration.isAlive()) {
                expirationListConfiguration.get().remove(remove);
            }
        }
    }

    public DataPayload pollLast(long timeoutMs) {
        DataPayload dataPayload = park.pollLast();
        if (dataPayload != null) {
            mapExpiration.put(dataPayload, expirationListConfiguration.get().add(dataPayload, timeoutMs));
            return dataPayload;
        }
        return null;
    }

    public DataPayload pollLast(long timeoutMs, long currentTimestamp) {
        DataPayload dataPayload = park.pollLast();
        if (dataPayload != null) {
            mapExpiration.put(
                    dataPayload,
                    expirationListConfiguration
                            .get()
                            .add(new ExpirationMsImmutableEnvelope<>(dataPayload, timeoutMs, currentTimestamp))
            );
            return dataPayload;
        }
        return null;
    }

    // Очередь полностью обработана. Нет
    public boolean isProcessed() {
        return finishState && position.isEmpty();
    }

    public DataPayload get(long position) {
        return this.position.get(position);
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of(new DataHeader()
                .addHeader("size", position.size())
        );
    }

}
