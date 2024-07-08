package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.extension.addable.AddToList;
import ru.jamsys.core.extension.property.PropertyType;
import ru.jamsys.core.extension.property.PropertyValue;
import ru.jamsys.core.extension.property.ValueName;
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

//Время срабатывания onExpired = 3 секунды

public class Broker<TEO>
        extends ExpirationMsMutableImpl
        implements
        ClassName,
        StatisticsFlush,
        Closable,
        CheckClassItem,
        LifeCycleInterface,
        KeepAlive,
        AddToList<
                ExpirationMsImmutableEnvelope<TEO>,
                DisposableExpirationMsImmutableEnvelope<TEO> // Должны вернуть, что бы из вне можно было сделать remove
                > {

    private final AtomicInteger queueSize = new AtomicInteger(0);

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
    final PropertyValue maxQueueSize;

    @Getter
    final PropertyValue maxTailQueueSize;

    final String index;

    private final Consumer<TEO> onDropConsumer;

    private final Class<TEO> classItem;

    private final Expiration<DisposableExpirationMsImmutableEnvelope> expiration;

    public Broker(String index, ApplicationContext applicationContext, Class<TEO> classItem, Consumer<TEO> onDropConsumer) {
        this.index = index;
        this.classItem = classItem;
        this.onDropConsumer = onDropConsumer;
        String clsIndex = getClassName(index, applicationContext);
        maxQueueSize = new PropertyValue(
                applicationContext,
                PropertyType.INTEGER,
                clsIndex + "." + ValueName.BROKER_SIZE.getName(),
                "3000"
        );

        maxTailQueueSize = new PropertyValue(
                applicationContext,
                PropertyType.INTEGER,
                clsIndex + "." + ValueName.BROKER_TAIL_SIZE.getName(),
                "3000"
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
        active();
        DisposableExpirationMsImmutableEnvelope<TEO> convert = DisposableExpirationMsImmutableEnvelope.convert(envelope);
        // Проблема с производительностью
        // Мы не можем использовать queue.size() для расчёта переполнения
        // пример: вставка 100к записей занимаем 35сек
        if (queueSize.get() >= maxQueueSize.getAsInt()) {
            // Он конечно протух не по своей воле, но что делать...
            // Как будто лучше его закинуть по стандартной цепочке, что бы операция была завершена
            DisposableExpirationMsImmutableEnvelope<TEO> teoDisposableExpirationMsImmutableEnvelope = queue.removeFirst();
            onDrop(teoDisposableExpirationMsImmutableEnvelope);
        }

        // Не важно есть onDropConsumer или нет, мы при помощи неё будем удалять сообщения из брокера
        expiration.add((DisposableExpirationMsImmutableEnvelope) convert);

        queue.add(convert);
        queueSize.incrementAndGet();
        if (tailQueue.size() >= maxTailQueueSize.getAsInt()) {
            tailQueue.pollFirst(); // с начала изымаем
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
            queueSize.decrementAndGet();
            return result.revert();
        } while (!queue.isEmpty());
        return null;
    }

    public void remove(DisposableExpirationMsImmutableEnvelope<TEO> envelope) {
        if (envelope != null) {
            // Делаем так, что бы он больше не достался никому
            TEO value = envelope.getValue();
            if (value != null) {
                statistic(envelope);
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
        return queueSize.get() * 100 / maxQueueSize.getAsInt();
    }

    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
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

    @Override
    public void close() {
        lastTimeInQueue = null;
    }

    // Рекомендуется использовать только для тестов
    public void reset() {
        queue.clear();
        queueSize.set(0);
        tailQueue.clear();
        tpsDequeue.set(0);
    }

    // Отладочная

    public List<TEO> getTailQueue(@Nullable AtomicBoolean isRun) {
        final List<TEO> ret = new ArrayList<>();
        UtilRisc.forEach(isRun, tailQueue, (ExpirationMsImmutableEnvelope<TEO> envelope) ->
                ret.add(envelope.getValue()));
        return ret;
    }

    public List<TEO> getCloneQueue(@Nullable AtomicBoolean isRun) {
        final List<TEO> cloned = new ArrayList<>();
        UtilRisc.forEach(isRun, queue, (DisposableExpirationMsImmutableEnvelope<TEO> envelope)
                -> cloned.add(envelope.revert().getValue()));
        return cloned;
    }

    @Override
    public boolean checkClassItem(Class<?> classItem) {
        return this.classItem.equals(classItem);
    }

    @Override
    public void run() {
        // Пока ничего не надо
    }

    @Override
    public void shutdown() {
        // Пока ничего не надо
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        // Если в очередь добавлять сообщения - будет вызываться active()
        // Брокер будет жить и при переполнении при вставке даже будет чистить очередь с начала
        // Но если очистка будет из вне при помощи remove или onDrop, да объекты будут обезврежены от
        // повторного использования, но ссылки в очереди останутся
        // Как решение пробегать с начала очереди, до момента получения не нейтрализованного объекта
        while (isThreadRun.get()) {
            // так как ConcurrentLinkedDeque.remove() идёт с first() - будем тоже работать с конца
            DisposableExpirationMsImmutableEnvelope<TEO> obj = queue.peekFirst();
            if (obj == null || !obj.isNeutralized()) {
                break;
            }
            queue.remove(obj);
        }
    }

}
