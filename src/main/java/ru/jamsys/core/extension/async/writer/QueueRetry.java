package ru.jamsys.core.extension.async.writer;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.expiration.ExpirationList;
import ru.jamsys.core.extension.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.extension.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

// Очередь данных, которые можно взять на обработку на какое-то время. Если за это время не выполнить remove, данные
// снова вернуться в очередь и будут повторно выданы

public class QueueRetry implements DataReader, StatisticsFlush, CascadeKey {

    @Getter
    @Setter
    private volatile boolean error = false; // Ошибка чтения данных

    @Getter
    @Setter
    private volatile boolean finishState; // Встретили -1 длину данных. -1 это EOF

    // Очередь в которую добавляют данные на обработку
    private final ConcurrentLinkedDeque<DataReadWrite> queue = new ConcurrentLinkedDeque<>();

    // Позиции
    private final ConcurrentHashMap<Long, DataReadWrite> waitPosition = new ConcurrentHashMap<>(); // key: position;

    private final String ns;

    @Getter
    private final ManagerConfiguration<ExpirationList<QueueRetryExpirationObject>> expirationListConfiguration;

    private final AtomicLong tpsEnq = new AtomicLong(0);

    private final AtomicLong tpsDeq = new AtomicLong(0);

    public QueueRetry(String ns, boolean finishState) {
        this.ns = ns;
        this.finishState = finishState;
        expirationListConfiguration = ManagerConfiguration.getInstance(
                App.getUniqueClassName(QueueRetry.class), // Это общий ExpirationList для всех экземпляров QueueRetry
                App.getUniqueClassName(QueueRetry.class), // Это общий ExpirationList для всех экземпляров QueueRetry
                ExpirationList.class,
                queueRetryExpirationObjectExpirationList -> queueRetryExpirationObjectExpirationList
                        .setupOnExpired(QueueRetryExpirationObject::insert)
        );
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                .append("finishState", finishState)
                .append("queue", queue.size())
                .append("waitPosition", waitPosition.size())
                ;
    }

    // Размер не обработанных элементов
    public int sizeWait() {
        return waitPosition.size();
    }

    @Override
    public void add(@NonNull DataReadWrite dataReadWrite) {
        // Хотел через computeIfAbsent, основа которого не вызывать исполнение лямды в случае отсутствия позиции,
        // но тогда надо делать атомики для контроля добавления и это больше исключение чем постоянная история
        if (waitPosition.putIfAbsent(dataReadWrite.getPosition(), dataReadWrite) == null) {
            tpsEnq.incrementAndGet();
            queue.add(dataReadWrite);
        } else {
            App.error(new RuntimeException("Duplicate DataReadWrite detected at position: " + dataReadWrite.getPosition()));
        }
    }

    public void remove(long position) {
        DataReadWrite dataReadWrite = this.waitPosition.remove(position);
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

    // Получить элемент на определённое время, если своевременно его не закоммитить (нейтролизовать) он снова добавится
    // в park
    public DataReadWrite pollLast(long timeoutMs) {
        return pollLast(timeoutMs, System.currentTimeMillis());
    }

    public DataReadWrite pollLast(long timeoutMs, long currentTimestamp) {
        DataReadWrite dataReadWrite;
        while (true) {
            dataReadWrite = queue.pollLast();
            if (dataReadWrite == null) {
                break;
            }
            if (!dataReadWrite.isRemove()) {
                QueueRetryExpirationObject foreignExpirationObject = new QueueRetryExpirationObject(
                        queue,
                        dataReadWrite
                );
                dataReadWrite.setExpiration(expirationListConfiguration
                        .get()
                        .add(new ExpirationMsImmutableEnvelope<>(foreignExpirationObject, timeoutMs, currentTimestamp))
                );
                tpsDeq.incrementAndGet();
                return dataReadWrite;
            }
        }
        return null;
    }

    // Очередь полностью обработана
    public boolean isProcessed() {
        return finishState && waitPosition.isEmpty();
    }

    // Только для unit тестов
    public DataReadWrite getForUnitTest(long position) {
        return this.waitPosition.get(position);
    }

    @Override
    public List<StatisticDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of(new StatisticDataHeader(getClass(), ns)
                .addHeader("size", queue.size())
                .addHeader("tpsEnq", tpsEnq.getAndSet(0))
                .addHeader("tpsDeq", tpsDeq.getAndSet(0))
                .addHeader("waitPosition", waitPosition.size())
        );
    }

}
