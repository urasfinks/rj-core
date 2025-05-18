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
        // Хотел через computeIfAbsent, основа которого не вызывать исполнение лямды в случае отсутствия позиции,
        // но тогда надо делать атомики для контроля добавления и это больше исключение чем постоянная история
        if (position.putIfAbsent(dataPayload.getPosition(), dataPayload) == null) {
            park.add(dataPayload);
        } else {
            App.error(new RuntimeException("Duplicate DataPayload detected at position: " + dataPayload.getPosition()));
        }
    }

    public void remove(long position) {
        DataPayload dataPayload = this.position.remove(position);
        if (dataPayload != null) {
            dataPayload.setRemove(true);
            DisposableExpirationMsImmutableEnvelope<DataPayload> expiration = dataPayload.getExpiration();
            if (expiration != null) {
                // Нейтрализуем что бы expirationList не взял его в обработку при timeout
                if (!expiration.doNeutralized()) {
                    App.error(new RuntimeException("Unfortunately, the block has already entered retry"));
                }
            }
        }
    }

    public DataPayload pollLast(long timeoutMs) {
        return pollLast(timeoutMs, System.currentTimeMillis());
    }

    public DataPayload pollLast(long timeoutMs, long currentTimestamp) {
        DataPayload dataPayload;
        while (true) {
            dataPayload = park.pollLast();
            if (dataPayload == null) {
                break;
            }
            if (!dataPayload.isRemove()) {
                dataPayload.setExpiration(expirationListConfiguration
                        .get()
                        .add(new ExpirationMsImmutableEnvelope<>(dataPayload, timeoutMs, currentTimestamp))
                );
                return dataPayload;
            }
        }
        return null;
    }

    // Очередь полностью обработана. Нет
    public boolean isProcessed() {
        return finishState && position.isEmpty();
    }

    // Только для unit тестов
    public DataPayload getForUnitTest(long position) {
        return this.position.get(position);
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of(new DataHeader()
                .addHeader("size", position.size())
        );
    }

}
