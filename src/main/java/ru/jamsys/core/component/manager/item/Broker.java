package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// Раньше на вставку был просто объект TEO и внутри происходила обёртка TimeEnvelope<TEO>
// Но потом пришла реализация Cache где нельзя было такое сделать
// и на вход надо было уже подавать объект TimeEnvelope<TEO>
//     Причина: сначала необходимо выставлять время, на которое будем кешировать, а только потом делать вставку
//              Индекс рассчитывается из времени expired
//              После установки время expired изменить нельзя, так как индекс уже ключом Map
// Я захотел, что бы везде было одинаково. Только лишь поэтому TEO -> TimeEnvelope<TEO>
// 11.05.2024 Cache переехал в Session, и вся история с однотипным протоколом распалась
// 26.05.2024 Session был удалён
// Спустя некоторое время появилась новая реализация Session)))
//Время срабатывания onExpired = 3 секунды

public class Broker<TEO>
        extends ExpirationMsMutableImpl
        implements
        UniqueClassName,
        StatisticsFlush,
        ClassEquals,
        LifeCycleInterface,
        KeepAlive,
        AddToList<
                ExpirationMsImmutableEnvelope<TEO>,
                DisposableExpirationMsImmutableEnvelope<TEO> // Должны вернуть, что бы из вне можно было сделать remove
                > {

    private final AtomicInteger queueSize = new AtomicInteger(0);

    private final AtomicInteger tailQueueSize = new AtomicInteger(0);

    private final ConcurrentLinkedDeque<DisposableExpirationMsImmutableEnvelope<TEO>> queue = new ConcurrentLinkedDeque<>();

    //Последний сообщения проходящие через очередь
    private final ConcurrentLinkedDeque<ExpirationMsImmutableEnvelope<TEO>> tailQueue = new ConcurrentLinkedDeque<>();

    // Я подумал, при деградации хорошо увидеть, что очередь вообще читается
    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AtomicInteger tpsDrop = new AtomicInteger(0);

    private final AvgMetric timeInQueue = new AvgMetric();

    @Getter
    private Double lastTimeInQueue;

    @Getter
    final Property<Integer> propertyBrokerSize;

    @Getter
    final Property<Integer> propertyBrokerTailSize;

    final String index;

    private final Consumer<TEO> onDropConsumer;

    private final Class<TEO> classItem;

    private final Expiration<DisposableExpirationMsImmutableEnvelope> expiration;

    public Broker(
            String index,
            ApplicationContext applicationContext,
            Class<TEO> classItem,
            Consumer<TEO> onDropConsumer
    ) {
        this.index = index;
        this.classItem = classItem;
        this.onDropConsumer = onDropConsumer;
        String clsIndex = getClassName(index, applicationContext);

        ServiceProperty serviceProperty = applicationContext.getBean(ServiceProperty.class);
        propertyBrokerSize = new Property<>(
                serviceProperty,
                Integer.class,
                clsIndex + "." + ValueName.BROKER_SIZE.getNameCamel(),
                3000,
                false,
                null
        );
        propertyBrokerTailSize = new Property<>(
                serviceProperty,
                Integer.class,
                clsIndex + "." + ValueName.BROKER_TAIL_SIZE.getNameCamel(),
                5,
                false,
                null
        );

        ManagerExpiration managerExpiration = applicationContext.getBean(ManagerExpiration.class);
        expiration = managerExpiration.get(
                getClassName(index, applicationContext),
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

    private void statistic(ExpirationMsImmutableEnvelope<TEO> envelope) {
        //#1, что бы видеть реальное кол-во опросов изъятия
        tpsDequeue.incrementAndGet();
        timeInQueue.add(envelope.getInactivityTimeMs());
    }

    public DisposableExpirationMsImmutableEnvelope<TEO> add(TEO element, long curTime, long timeOut) {
        return add(new ExpirationMsImmutableEnvelope<>(element, timeOut, curTime));
    }

    public DisposableExpirationMsImmutableEnvelope<TEO> add(TEO element, long timeOutMs) {
        return add(new ExpirationMsImmutableEnvelope<>(element, timeOutMs));
    }

    @Override
    public DisposableExpirationMsImmutableEnvelope<TEO> add(ExpirationMsImmutableEnvelope<TEO> envelope) {
        if (envelope == null || envelope.isExpired()) {
            return null;
        }
        setActivity();
        DisposableExpirationMsImmutableEnvelope<TEO> convert = DisposableExpirationMsImmutableEnvelope.convert(envelope);
        // Проблема с производительностью
        // Мы не можем использовать queue.size() для расчёта переполнения
        // пример: вставка 100к записей занимаем 35сек
        if (queueSize.get() >= propertyBrokerSize.get()) {
            // Он конечно протух не по своей воле, но что делать...
            // Как будто лучше его закинуть по стандартной цепочке, что бы операция была завершена
            DisposableExpirationMsImmutableEnvelope<TEO> teoDisposableExpirationMsImmutableEnvelope = queue.removeFirst();
            onDrop(teoDisposableExpirationMsImmutableEnvelope);
        }

        // Не важно есть onDropConsumer или нет, мы при помощи неё будем удалять сообщения из брокера
        expiration.add((DisposableExpirationMsImmutableEnvelope) convert);

        queue.add(convert);
        queueSize.incrementAndGet();

        if (tailQueueSize.get() >= propertyBrokerTailSize.get()) {
            tailQueue.removeFirst();
        } else {
            tailQueueSize.incrementAndGet();
        }
        tailQueue.add(envelope);
        return convert;
    }

    public ExpirationMsImmutableEnvelope<TEO> pollFirst() {
        return pool(true);
    }

    public ExpirationMsImmutableEnvelope<TEO> pollLast() {
        return pool(false);
    }

    private ExpirationMsImmutableEnvelope<TEO> pool(boolean first) {
        do {
            DisposableExpirationMsImmutableEnvelope<TEO> result = first ? queue.pollFirst() : queue.pollLast();
            if (result == null) {
                return null;
            }
            if (result.isExpired()) {
                onDrop(result);
                continue;
            }
            TEO value = result.getValue();
            if (value == null) {
                continue;
            }
            statistic(result);
            // Все операции делаем в конце, когда получаем нейтрализованный объект
            // Так как многопоточная среда, могут выхватить из под носа
            queueSize.decrementAndGet();
            return result.revert();
        } while (!queue.isEmpty());
        return null;
    }

    public void remove(DisposableExpirationMsImmutableEnvelope<TEO> envelope) {
        if (envelope != null) {
            // Это конечно так себе удалять пришедший в remove объект не проверяя что он вообще есть в очереди
            // Но как бы проверять  наличие - это перебирать всё очередь, а то очень тяжело
            // Просто доверяем, что брокеры не перепутают
            // Делаем так, что бы элемент больше не достался никому
            TEO value = envelope.getValue();
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
    public void onDrop(DisposableExpirationMsImmutableEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        TEO value = (TEO) envelope.getValue();
        if (value != null) {
            queueSize.decrementAndGet();
            tpsDrop.incrementAndGet();
            if (onDropConsumer != null) {
                onDropConsumer.accept(value);
            }
        }
    }

    // Получить процент заполненности очереди
    public int getOccupancyPercentage() {
        //  MAX - 100
        //  500 - x
        return queueSize.get() * 100 / propertyBrokerSize.get();
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

    public List<TEO> getTailQueue(@Nullable AtomicBoolean run) {
        final List<TEO> ret = new ArrayList<>();
        UtilRisc.forEach(run, tailQueue, (ExpirationMsImmutableEnvelope<TEO> envelope) ->
                ret.add(envelope.getValue()));
        return ret;
    }

    public List<TEO> getCloneQueue(@Nullable AtomicBoolean run) {
        final List<TEO> cloned = new ArrayList<>();
        UtilRisc.forEach(run, queue, (DisposableExpirationMsImmutableEnvelope<TEO> envelope)
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
            DisposableExpirationMsImmutableEnvelope<TEO> obj = queue.peekFirst();
            if (obj == null || !obj.isNeutralized()) {
                break;
            }
            queue.remove(obj);
        }
    }

    @Override
    public void run() {
        propertyBrokerSize.run();
        propertyBrokerTailSize.run();
    }

    @Override
    public void shutdown() {
        propertyBrokerSize.run();
        propertyBrokerTailSize.run();
        lastTimeInQueue = null;
    }

}
