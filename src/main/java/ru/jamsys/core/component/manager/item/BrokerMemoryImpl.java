package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.extension.ClassEquals;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.extension.broker.persist.BrokerMemory;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// Брокер - циклическая очередь элементов, с timeout элементов (onExpired = 3 секунды)
// Не является CascadeName - используйте каскадные имена в ключе

public class BrokerMemoryImpl<T>
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        BrokerMemory<T>,
        StatisticsFlush,
        ClassEquals,
        LifeCycleInterface,
        KeepAlive,
        AddToList<
                ExpirationMsImmutableEnvelope<T>,
                DisposableExpirationMsImmutableEnvelope<T> // Должны вернуть, что бы из вне можно было сделать remove
                > {


    private final AtomicInteger queueSize = new AtomicInteger(0);

    private final AtomicInteger tailQueueSize = new AtomicInteger(0);

    private final ConcurrentLinkedDeque<DisposableExpirationMsImmutableEnvelope<T>> queue = new ConcurrentLinkedDeque<>();

    //Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<ExpirationMsImmutableEnvelope<T>> tailQueue = new ConcurrentLinkedDeque<>();

    // Я подумал, при деградации хорошо увидеть, что очередь вообще читается
    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AtomicInteger tpsDrop = new AtomicInteger(0);

    private final AvgMetric timeInQueue = new AvgMetric();

    @Getter
    private Double lastTimeInQueue;

    @Getter
    final String key;

    private final Consumer<T> onDrop;

    private final Class<T> classItem;

    @SuppressWarnings("all")
    private final Expiration<DisposableExpirationMsImmutableEnvelope> expiration;

    @Getter
    private final BrokerProperty propertyBroker = new BrokerProperty();

    @Getter
    private final PropertyDispatcher<Integer> propertyDispatcher;

    public BrokerMemoryImpl(
            String key,
            ApplicationContext applicationContext,
            Class<T> classItem,
            Consumer<T> onDrop
    ) {
        this.key = key;
        this.classItem = classItem;
        this.onDrop = onDrop;

        ServiceProperty serviceProperty = applicationContext.getBean(ServiceProperty.class);
        propertyDispatcher = new PropertyDispatcher<>(
                serviceProperty,
                null,
                getPropertyBroker(),
                key
        );

        expiration = applicationContext.getBean(ManagerExpiration.class).get(
                key,
                DisposableExpirationMsImmutableEnvelope.class,
                this::onDrop
        );
    }

    public int size() {
        return queueSize.get();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    private void statistic(ExpirationMsImmutableEnvelope<T> envelope) {
        //#1, что бы видеть реальное кол-во опросов изъятия
        tpsDequeue.incrementAndGet();
        timeInQueue.add(envelope.getInactivityTimeMs());
    }

    @Override
    public DisposableExpirationMsImmutableEnvelope<T> add(ExpirationMsImmutableEnvelope<T> envelope) {
        if (envelope == null || envelope.isExpired()) {
            return null;
        }
        setActivity();
        DisposableExpirationMsImmutableEnvelope<T> convert = DisposableExpirationMsImmutableEnvelope.convert(envelope);
        // Проблема с производительностью
        // Мы не можем использовать queue.size() для расчёта переполнения
        // пример: вставка 100к записей занимаем 35сек
        if (queueSize.get() >= getPropertyBroker().getSize()) {
            // Он конечно протух не по своей воле, но что делать...
            // Как будто лучше его закинуть по стандартной цепочке, что бы операция была завершена
            DisposableExpirationMsImmutableEnvelope<T> teoDisposableExpirationMsImmutableEnvelope = queue.removeFirst();
            onDrop(teoDisposableExpirationMsImmutableEnvelope);
        }

        // Не важно есть onDropConsumer или нет, мы при помощи её будем удалять сообщения из брокера
        expiration.add((DisposableExpirationMsImmutableEnvelope) convert);

        queue.add(convert);
        queueSize.incrementAndGet();

        if (tailQueueSize.get() >= getPropertyBroker().getTailSize()) {
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
            DisposableExpirationMsImmutableEnvelope<T> result = first ? queue.pollFirst() : queue.pollLast();
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
            statistic(result);
            // Все операции делаем в конце, когда получаем нейтрализованный объект
            // Так как многопоточная среда, могут выхватить из-под носа
            queueSize.decrementAndGet();
            return result.revert();
        } while (!queue.isEmpty());
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
                statistic(envelope);
                // Когда явно получили эксклюзивный доступ к объекту - можно и статистику посчитать
                queueSize.decrementAndGet();
                // Объект уже нейтрализован, поэтому просто его удаляем из expiration
                expiration.remove((DisposableExpirationMsImmutableEnvelope) envelope, false);
            }
        }
    }

    //Обработка выпадающих сообщений
    @SuppressWarnings("all")
    private void onDrop(DisposableExpirationMsImmutableEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        T value = (T) envelope.getValue();
        if (value != null) {
            queueSize.decrementAndGet();
            tpsDrop.incrementAndGet();
            if (onDrop != null) {
                onDrop.accept(value);
            }
        }
    }

    // Получить процент заполненности очереди
    public int getOccupancyPercentage() {
        //  MAX - 100
        //  500 - x
        return queueSize.get() * 100 / getPropertyBroker().getSize();
    }

    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean threadRun) {
        List<Statistic> result = new ArrayList<>();
        int tpsDequeueFlush = tpsDequeue.getAndSet(0);
        int tpsDropFlush = tpsDrop.getAndSet(0);
        int sizeFlush = queueSize.get();
        Map<String, Object> flush = timeInQueue.flush("time");
        lastTimeInQueue = (Double) flush.get("timeAvg");
        result.add(new Statistic(parentTags, parentFields)
                .addField("tpsDeq", tpsDequeueFlush)
                .addField("tpsDrop", tpsDropFlush)
                .addField("size", sizeFlush)
                .addFields(flush)
        );
        return result;
    }

    // Рекомендуется использовать только для тестов
    public void reset() {
        queue.clear();
        queueSize.set(0);
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
        UtilRisc.forEach(run, queue, (DisposableExpirationMsImmutableEnvelope<T> envelope)
                -> cloned.add(envelope.revert().getValue()));
        return cloned;
    }

    @Override
    public boolean classEquals(Class<?> classItem) {
        return this.classItem.equals(classItem);
    }

    @Override
    public void keepAlive(AtomicBoolean threadRun) {
        // Если в очередь добавлять сообщения - будет вызываться active()
        // Брокер будет жить и при переполнении при вставке даже будет чистить очередь с начала
        // Но если очистка будет из вне при помощи remove или onDrop, да объекты будут обезврежены от
        // повторного использования, но ссылки в очереди останутся
        // Как решение пробегать с начала очереди, до момента получения не нейтрализованного объекта
        while (threadRun.get()) {
            // так как ConcurrentLinkedDeque.remove() идёт с first() - будем тоже работать с конца
            DisposableExpirationMsImmutableEnvelope<T> obj = queue.peekFirst();
            if (obj == null || !obj.isNeutralized()) {
                break;
            }
            queue.remove(obj);
        }
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
        lastTimeInQueue = null;
    }

}
