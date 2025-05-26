package ru.jamsys.core.extension.broker.memory;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.springframework.lang.Nullable;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.extension.broker.BrokerRepositoryProperty;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// Брокер - циклическая очередь элементов, с timeout элементов в оперативной памяти

public class BrokerMemory<T>
        extends AbstractBrokerMemory<T>
        implements
        AddToList<
                ExpirationMsImmutableEnvelope<T>,
                DisposableExpirationMsImmutableEnvelope<T> // Должны вернуть, что бы из вне можно было сделать remove
                > {


    private final AtomicInteger mainQueueSize = new AtomicInteger(0);

    private final AtomicInteger tailQueueSize = new AtomicInteger(0);

    // Основная очередь сообщений
    private final ConcurrentLinkedDeque<DisposableExpirationMsImmutableEnvelope<T>> mainQueue = new ConcurrentLinkedDeque<>();

    // Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<ExpirationMsImmutableEnvelope<T>> tailQueue = new ConcurrentLinkedDeque<>();

    // Я подумал, при деградации хорошо увидеть, что очередь вообще читается
    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AtomicInteger tpsDrop = new AtomicInteger(0);

    private final AvgMetric timeInQueue = new AvgMetric();

    @Getter
    final String ns;

    private Consumer<T> onPostDrop; // Если установлено, вызывается после вы

    @Getter
    private final BrokerRepositoryProperty property = new BrokerRepositoryProperty();

    @Getter
    private final PropertyDispatcher<Integer> propertyDispatcher;

    public BrokerMemory(
            String ns
    ) {
        this.ns = ns;
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                getProperty(),
                getCascadeKey(ns)
        );
    }

    public void setupOnDrop(Consumer<T> onDrop){
        this.onPostDrop = onDrop;
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                //.append("brokerRepositoryProperty", property)
                ;
    }

    public long size() {
        return mainQueueSize.get();
    }

    public boolean isEmpty() {
        return mainQueue.isEmpty();
    }

    @Override
    public DisposableExpirationMsImmutableEnvelope<T> add(ExpirationMsImmutableEnvelope<T> envelope) {
        if (envelope == null || envelope.isExpired()) {
            UtilLog.printError(envelope);
            return null;
        }
        isNotRunThrow();
        markActive();
        DisposableExpirationMsImmutableEnvelope<T> convert = DisposableExpirationMsImmutableEnvelope.convert(envelope);
        // Проблема с производительностью
        // Мы не можем использовать queue.size() для расчёта переполнения
        // пример: вставка 100к записей занимаем 35сек
        if (mainQueueSize.get() >= getProperty().getSize()) {
            // Он конечно протух не по своей воле, но что делать...
            // Как будто лучше его закинуть по стандартной цепочке, что бы операция была завершена
            onDrop(mainQueue.removeFirst());
        }

        mainQueue.add(convert);
        mainQueueSize.incrementAndGet();

        if (tailQueueSize.get() >= getProperty().getTailSize()) {
            tailQueue.removeFirst();
        } else {
            tailQueueSize.incrementAndGet();
        }
        tailQueue.add(envelope);
        return convert;
    }

    public ExpirationMsImmutableEnvelope<T> pollFirst() {
        return pool(true);
    }

    public ExpirationMsImmutableEnvelope<T> pollLast() {
        return pool(false);
    }

    private ExpirationMsImmutableEnvelope<T> pool(boolean first) {
        do {
            DisposableExpirationMsImmutableEnvelope<T> result = first ? mainQueue.pollFirst() : mainQueue.pollLast();
            if (result == null) {
                return null;
            }
            if (result.isExpired()) {
                onDrop(result);
                continue;
            }
            T value = result.getValue();
            if (value == null) {
                continue;
            }
            onQueueLoss(result);
            // Все операции делаем в конце, когда получаем нейтрализованный объект
            // Так как многопоточная среда, могут выхватить из-под носа
            mainQueueSize.decrementAndGet();
            return result.revert();
        } while (!mainQueue.isEmpty());
        return null;
    }

    public void remove(DisposableExpirationMsImmutableEnvelope<T> envelope) {
        if (envelope != null) {
            // Это конечно так себе удалять пришедший в remove объект не проверяя что он вообще есть в очереди
            // Но как бы проверять наличие - это перебирать всё очередь, а то очень тяжело
            // Просто доверяем, что брокеры не перепутают.
            // Делаем так, что бы элемент больше не достался никому
            T value = envelope.getValue();
            if (value != null) {
                onQueueLoss(envelope);
                // Когда явно получили эксклюзивный доступ к объекту - можно и статистику посчитать
                mainQueueSize.decrementAndGet();
            }
        }
    }

    // Когда произошло изъятие элемента из очереди любым способом
    private void onQueueLoss(ExpirationMsImmutableEnvelope<T> envelope) {
        markActive();
        tpsDequeue.incrementAndGet();
        timeInQueue.add(envelope.getInactivityTimeMs());
    }

    //Обработка выпадающих сообщений
    @SuppressWarnings("all")
    public void onDrop(DisposableExpirationMsImmutableEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        T value = (T) envelope.getValue();
        if (value != null) {
            // На самомом деле мы не удаляем из очереди, однако вызываем onQueueLoss, что бы увеличть счётчик на вставку
            // так как бегать по очереди в поисках элемента - накладная операция, когда poll дойдёт до этого элемента
            // он просто провернёт через continue его. При этом вставку в очередь будем разрешать, не смотря на
            // то что в очереди может находится реально больше элементов, чем положено.
            onQueueLoss(envelope);
            tpsDrop.incrementAndGet();
            if (onPostDrop != null) {
                onPostDrop.accept(value);
            }
        }
    }

    // Получить процент заполненности очереди
    public int getOccupancyPercentage() {
        //  MAX - 100
        //  500 - x
        return (int) (((float) mainQueueSize.get()) * 100 / getProperty().getSize());
    }

    // Рекомендуется использовать только для тестов
    public void reset() {
        mainQueue.clear();
        mainQueueSize.set(0);
        tailQueue.clear();
        tpsDequeue.set(0);
        tailQueueSize.set(0);
    }

    // Отладочная
    public List<T> getTailQueue(@Nullable AtomicBoolean run) {
        final List<T> ret = new ArrayList<>();
        UtilRisc.forEach(run, tailQueue, (ExpirationMsImmutableEnvelope<T> envelope) ->
                ret.add(envelope.getValue()));
        return ret;
    }

    public List<T> getCloneQueue(@Nullable AtomicBoolean run) {
        final List<T> cloned = new ArrayList<>();
        UtilRisc.forEach(run, mainQueue, (DisposableExpirationMsImmutableEnvelope<T> envelope)
                -> cloned.add(envelope.revert().getValue()));
        return cloned;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        // Ничего светлого больше не будет, если объект останавливают. Что бы хоть как-то отразить свою недосказанность
        // запихаем все не считанные сообщения в onDrop
        if (onPostDrop != null) {
            while (!mainQueue.isEmpty()) {
                DisposableExpirationMsImmutableEnvelope<T> poll = mainQueue.pollFirst();
                if (poll != null) {
                    T value = poll.getValue();
                    if (value != null) {
                        onPostDrop.accept(value);
                    }
                }
            }
        }
        propertyDispatcher.shutdown();
    }

    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<DataHeader> result = new ArrayList<>();
        int tpsDequeueFlush = tpsDequeue.getAndSet(0);
        int tpsDropFlush = tpsDrop.getAndSet(0);
        int sizeFlush = mainQueueSize.get();
        result.add(new DataHeader()
                .setBody(getCascadeKey(ns))
                .addHeader("tpsDeq", tpsDequeueFlush)
                .addHeader("tpsDrop", tpsDropFlush)
                .addHeader("size", sizeFlush)
                .addHeader("avg", timeInQueue.flushStatistic())
        );
        return result;
    }

}
