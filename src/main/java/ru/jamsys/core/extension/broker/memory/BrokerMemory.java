package ru.jamsys.core.extension.broker.memory;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.springframework.lang.Nullable;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.extension.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.statistic.AvgMetric;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.flat.util.UtilRisc;

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
        CascadeKey,
        AddToList<
                ExpirationMsImmutableEnvelope<T>,
                DisposableExpirationMsImmutableEnvelope<T> // Должны вернуть, что бы из вне можно было сделать remove
                > {

    // Основная очередь сообщений
    private final ConcurrentLinkedDeque<DisposableExpirationMsImmutableEnvelope<T>> mainQueue = new ConcurrentLinkedDeque<>();

    // Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<ExpirationMsImmutableEnvelope<T>> tailQueue = new ConcurrentLinkedDeque<>();

    private final AtomicInteger mainQueueSize = new AtomicInteger(0);

    private final AtomicInteger tailQueueSize = new AtomicInteger(0);

    private final AtomicInteger tpsEnqueue = new AtomicInteger(0);

    // Я подумал, при деградации хорошо увидеть, что очередь вообще читается
    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AtomicInteger tpsDrop = new AtomicInteger(0);

    private final AvgMetric timeInQueue = new AvgMetric();

    @Getter
    final String ns;

    private Consumer<T> onPostDrop; // Если установлено, вызывается после вы

    @Getter
    private final BrokerMemoryRepositoryProperty property = new BrokerMemoryRepositoryProperty();

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

    public void setup(Consumer<T> onDrop){
        this.onPostDrop = onDrop;
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                .append("propertyDispatcherNs", propertyDispatcher.getNs())
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
        DisposableExpirationMsImmutableEnvelope<T> convert = DisposableExpirationMsImmutableEnvelope.convert(envelope);
        // Проблема с производительностью
        // Мы не можем использовать queue.size() для расчёта переполнения
        // пример: вставка 100к записей занимаем 35сек
        if (mainQueueSize.get() >= getProperty().getSize()) {
            DisposableExpirationMsImmutableEnvelope<T> removedFirst = mainQueue.removeFirst();
            if (removedFirst != null) {
                T value = removedFirst.getValue();
                if (value != null) {
                    mainQueueSize.decrementAndGet();
                    timeInQueue.add(removedFirst.getDurationSinceLastActivityMs());
                    onDrop(value);
                }
            }
        }

        mainQueue.add(convert);
        mainQueueSize.incrementAndGet();
        tpsEnqueue.incrementAndGet();

        if (tailQueueSize.get() >= getProperty().getTailSize()) {
            tailQueue.removeFirst();
        } else {
            tailQueueSize.incrementAndGet();
        }
        tailQueue.add(envelope);
        return convert;
    }

    // Для очереди в памяти всегда изъятие происходит с конца, для изъятия сначала использовать BrokerPersist.
    // Причина очень простая, любое замедление системы и вычитывание с начала приводит к порочному кругу, когда
    // обрабатывая с начала времени не будет успевать обработать их, а в это время в конце очереди уже тратится время
    // в итоге и старые не хватает времени обработать и новые тухнут к моменту их обработке. LoginStorm - это когда
    // происходит увеличение нагрузки из-за недоступности системы, пользователи получают ошибки и пытаются снова и
    // снова сделать проводку, тем самым увеличивая длину очереди и в случае обработки сначала - система не сможет это
    // всё обработать и будет находиться в режиме постоянного протухания
    public ExpirationMsImmutableEnvelope<T> poll() {
        do {
            DisposableExpirationMsImmutableEnvelope<T> result = mainQueue.pollLast();
            tpsDequeue.incrementAndGet();
            if (result == null) {
                return null;
            }
            if (result.isExpired()) {
                T value = result.getValue();
                if (value != null) {
                    mainQueueSize.decrementAndGet();
                    timeInQueue.add(result.getDurationSinceLastActivityMs());
                    onDrop(value);
                }
                continue;
            }
            T value = result.getValue();
            if (value == null) {
                continue;
            }
            mainQueueSize.decrementAndGet();
            timeInQueue.add(result.getDurationSinceLastActivityMs());
            return result.revert();
        } while (!mainQueue.isEmpty());
        return null;
    }

    public void helper(AtomicBoolean threadRun) {
        while (threadRun.get()) {
            DisposableExpirationMsImmutableEnvelope<T> peekResult = mainQueue.peekFirst();
            if (peekResult == null) {
                break;
            }
            if (peekResult.isExpired()) {
                // Удаление происходит через поиск с начала очереди (removeFirstOccurrence(o)), так что это будет очень
                // быстро
                if (mainQueue.remove(peekResult)) {
                    T value = peekResult.getValue();
                    if (value != null) {
                        mainQueueSize.decrementAndGet();
                        timeInQueue.add(peekResult.getDurationSinceLastActivityMs());
                    }
                }
            } else {
                break;
            }
        }
    }

    public void remove(DisposableExpirationMsImmutableEnvelope<T> envelope) {
        if (envelope != null) {
            // Это конечно так себе удалять пришедший в remove объект не проверяя что он вообще есть в очереди
            // Но как бы проверять наличие - это перебирать всё очередь, а то очень тяжело
            // Просто доверяем, что брокеры не перепутают.
            // Делаем так, что бы элемент больше не достался никому
            T value = envelope.getValue();
            if (value != null) {
                mainQueueSize.decrementAndGet();
                timeInQueue.add(envelope.getDurationSinceLastActivityMs());
            }
        }
    }

    //Обработка выпадающих сообщений
    @SuppressWarnings("all")
    public void onDrop(T value) {
        tpsDrop.incrementAndGet();
        if (onPostDrop != null) {
            onPostDrop.accept(value);
        }
    }

    // Получить процент заполненности очереди
    @SuppressWarnings("unused")
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
                DisposableExpirationMsImmutableEnvelope<T> pollFirst = mainQueue.pollFirst();
                if (pollFirst != null) {
                    T value = pollFirst.getValue();
                    if (value != null) {
                        mainQueueSize.decrementAndGet();
                        timeInQueue.add(pollFirst.getDurationSinceLastActivityMs());
                        onDrop(value);
                    }
                }
            }
        }
        propertyDispatcher.shutdown();
    }

    public List<StatisticDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<StatisticDataHeader> result = new ArrayList<>();
        AvgMetric.Statistic statistic = timeInQueue.flushStatistic();
        result.add(new StatisticDataHeader(getClass(), ns)
                .addHeader("tpsEnq", tpsEnqueue.getAndSet(0))
                .addHeader("tpsDeq", tpsDequeue.getAndSet(0))
                .addHeader("tpsDrop", tpsDrop.getAndSet(0))
                .addHeader("size", mainQueueSize.get())
                .addHeader("timeInQueue", statistic.getAvg())
                .addHeader("timeInQueue.min", statistic.getMin())
                .addHeader("timeInQueue.max", statistic.getMax())
                .addHeader("timeInQueue.count", statistic.getCount())
                .addHeader("timeInQueue.sum", statistic.getSum())
        );
        if (!mainQueue.isEmpty()) {
            markActive();
        }
        return result;
    }

}
