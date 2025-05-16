package ru.jamsys.core.extension.batch.writer;

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
import java.util.concurrent.atomic.AtomicInteger;

// Очередь данных, которые можно взять на обработку на какое-то время. Если за это время не выполнить commit, данные
// снова вернуться в очередь и будут повторно выданы

public class QueueRetry implements DataFromFile, StatisticsFlush {

    @Getter
    @Setter
    private volatile boolean error = false; // Ошибка чтения данных

    @Getter
    @Setter
    private volatile boolean finishState = false; // Встретили -1 длину данных в bin

    @Getter
    private final ConcurrentLinkedDeque<DataPayload> queue = new ConcurrentLinkedDeque<>();

    private final ConcurrentHashMap<Long, DataPayload> unique = new ConcurrentHashMap<>(); // key: position;

    private final AtomicInteger queueSize = new AtomicInteger();

    private final AtomicInteger polled = new AtomicInteger();

    @Getter
    private final Manager.Configuration<ExpirationList<DataPayload>> expirationListConfiguration;

    private final Map<DataPayload, DisposableExpirationMsImmutableEnvelope<DataPayload>> mapExpiration = new ConcurrentHashMap<>();

    public QueueRetry(String key) {
        expirationListConfiguration = App.get(Manager.class).configureGeneric(
                ExpirationList.class,
                key,
                key1 -> new ExpirationList<>(
                        key1,
                        exp -> {
                            queue.add(exp.getValue());
                            queueSize.incrementAndGet();
                        }
                )
        );
    }

    public int size() {
        return queueSize.get();
    }

    @Override
    public void add(@NonNull DataPayload dataPayload) {
        unique.computeIfAbsent(dataPayload.getPosition(), _ -> {
            queue.add(dataPayload);
            queueSize.incrementAndGet();
            return dataPayload;
        });
    }

    public void remove(long position) {
        DataPayload dataPayload = unique.remove(position);
        if (dataPayload != null && queue.remove(dataPayload)) {
            queueSize.decrementAndGet();
            commit(dataPayload);
        }
    }

    public void commit(DataPayload item) {
        DisposableExpirationMsImmutableEnvelope<DataPayload> remove = mapExpiration.remove(item);
        if (remove != null && expirationListConfiguration.isAlive()) {
            expirationListConfiguration.get().remove(remove);
        }
    }

    public DataPayload pollLast(long timeoutMs) {
        DataPayload dataPayload = queue.pollLast();
        if (dataPayload != null) {
            queueSize.decrementAndGet();
            polled.incrementAndGet();
            mapExpiration.put(dataPayload, expirationListConfiguration.get().add(dataPayload, timeoutMs));
            return dataPayload;
        }
        return null;
    }

    public DataPayload pollFirst(long timeoutMs) {
        DataPayload dataPayload = queue.pollFirst();
        if (dataPayload != null) {
            queueSize.decrementAndGet();
            polled.incrementAndGet();
            mapExpiration.put(dataPayload, expirationListConfiguration.get().add(dataPayload, timeoutMs));
            return dataPayload;
        }
        return null;
    }

    public DataPayload poll(long timeoutMs, long nowTimestamp) {
        DataPayload dataPayload = queue.pollLast();
        if (dataPayload != null) {
            queueSize.decrementAndGet();
            polled.incrementAndGet();
            mapExpiration.put(
                    dataPayload,
                    expirationListConfiguration
                            .get()
                            .add(new ExpirationMsImmutableEnvelope<>(dataPayload, timeoutMs, nowTimestamp))
            );
            return dataPayload;
        }
        return null;
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of(new DataHeader()
                .addHeader("queueSize", queueSize.get())
                .addHeader("polled", polled.getAndSet(0))
        );
    }

    public boolean isEmpty() {
        // Если ExpirationList жив, надо проверить, что он пуст
        if (expirationListConfiguration.isAlive()) {
            return queue.isEmpty() && expirationListConfiguration.get().isEmpty();
        } else {
            // При остановке ExpirationList происходит clear(), поэтому нет необходимости проверять его пустоту
            // Надо проверить только queue на пустоту
            return queue.isEmpty();
        }
    }

    public DataPayload get(long l) {
        return unique.get(l);
    }

}
