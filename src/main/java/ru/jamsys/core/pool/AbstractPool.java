package ru.jamsys.core.pool;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.resource.ResourceCheckException;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

// RA - ResourceArguments
// R - ResourceResult
// PI - PoolItem

// Отработанные пловцы добавляются в парк в конец очереди
// На работу пловцы отправляются из парка с конца очереди
// Таким образом в парке в начале очереди будут тушиться пловцы без работы до их кончины

// TODO: а что если ресурс не вернуть в пул? Надо сделать какое-то время адекватное - например 1 час
// и перезагружать, но надо точно знать - что именно ресурс был взять и не возвращён ни разу за последнее время


@ToString(onlyExplicitlyIncluded = true)
public abstract class AbstractPool<T extends ExpirationMsMutable & Valid & ResourceCheckException>
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements Pool<T>, LifeCycleInterface, CascadeKey {

    public static Set<AbstractPool<?>> registerPool = Util.getConcurrentHashSet();

    @Getter
    @ToString.Include
    protected final String ns;

    private final ConcurrentLinkedDeque<T> parkQueue = new ConcurrentLinkedDeque<>();

    protected final ConcurrentLinkedQueue<T> removeQueue = new ConcurrentLinkedQueue<>();

    protected final ConcurrentLinkedQueue<T> exceptionQueue = new ConcurrentLinkedQueue<>();

    // Общая очередь, где находятся все объекты
    protected final Set<T> itemQueue = Util.getConcurrentHashSet();

    @Setter
    private Function<Integer, Integer> formulaAddCount = (need) -> need;

    @Setter
    private Function<Integer, Integer> formulaRemoveCount = (need) -> need;

    private volatile Long timeWhenParkIsEmpty = null;

    // Используется в виртуальных пулах потоков в RealThreadComponent, что бы не держать пулы для задач, которые уже не
    // исполняются по каким либо причинам
    private final AtomicInteger tpsComplete = new AtomicInteger(0);

    private final AtomicBoolean dynamicPollSize = new AtomicBoolean(false);

    private final Lock lockAddToPark = new ReentrantLock();

    private final Lock lockAddToRemove = new ReentrantLock();

    @Getter
    private final PoolProperty poolProperty = new PoolProperty();

    @Getter
    private final PropertyDispatcher<Integer> propertyDispatcher;

    public AbstractPool(String ns) {
        this.ns = ns;
        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                null,
                poolProperty,
                getCascadeKey(ns)
        );
    }

    public boolean isParkQueueEmpty() {
        return parkQueue.isEmpty();
    }

    @SuppressWarnings("unused")
    public void setDynamicPollSize(boolean dynamic) {
        dynamicPollSize.set(dynamic);
    }

    // Сколько времени паркинг был пуст
    public long getTimeParkIsEmpty() {
        if (timeWhenParkIsEmpty == null) {
            return 0;
        } else {
            return System.currentTimeMillis() - timeWhenParkIsEmpty;
        }
    }

    protected void updateParkStatistic() {
        if (parkQueue.isEmpty()) {
            if (timeWhenParkIsEmpty == null) {
                timeWhenParkIsEmpty = System.currentTimeMillis();
            }
        } else {
            timeWhenParkIsEmpty = null;
        }
    }

    public boolean isEmpty() {
        return itemQueue.isEmpty();
    }

    @SuppressWarnings("unused")
    public void setPoolSizeMax(int want) {
        if (!dynamicPollSize.get()) {
            return;
        }
        // Если хотят меньше минимума - очень резко опускаем максимум до минимума
        if (want < poolProperty.getMin()) {
            App.get(ServiceProperty.class).set(
                    propertyDispatcher
                            .getPropertyRepository()
                            .getByFieldNameConstants(PoolProperty.Fields.max)
                            .getPropertyKey(),
                    poolProperty.getMin()
            );
            return;
        }
        // Если желаемое значение элементов в пуле больше минимума, так как return не сработал
        if (want > poolProperty.getMax()) { //Медленно поднимаем
            App.get(ServiceProperty.class).set(
                    propertyDispatcher
                            .getPropertyRepository()
                            .getByFieldNameConstants(PoolProperty.Fields.max)
                            .getPropertyKey(),
                    poolProperty.getMax() + 1
            );
        } else { //Но очень быстро опускаем
            App.get(ServiceProperty.class).set(
                    propertyDispatcher
                            .getPropertyRepository()
                            .getByFieldNameConstants(PoolProperty.Fields.max)
                            .getPropertyKey(),
                    want
            );
        }
    }

    // Проверка, что пул может поместить новые объекты
    private boolean isSizePoolAllowsExtend() {
        if (!isRun()) {
            App.error(new RuntimeException("Пул " + ns + " не может поместить в себя ничего, так как он выключен"));
        }
        return isRun() && getRealActiveItem() < poolProperty.getMax();
    }

    private int getRealActiveItem() {
        // Активные пловцы это те, которые не под ножом и не ошибочные
        return itemQueue.size() - (removeQueue.size() + exceptionQueue.size());
    }

    // Увеличиваем кол-во объектов для плаванья
    private void overclocking(long count) {
        for (int i = 0; i < count; i++) {
            if (!add()) {
                break;
            }
        }
    }

    public boolean isAvailablePoolItem() {
        if (!parkQueue.isEmpty()) { // Если в парке есть ресурсы, сразу говорим что всё ок
            return true;
        }
        // Если всё-таки в парке нет никого проверяем возможность создать новый элемент
        // но только в том случае, если настройки парка могут быть равны 0 и кол-во активных элементов равны 0
        // Парк типо просто из-за неактивности ушёл в ноль, что бы не тратить ресурсы
        // иначе вернём false и просто задачи будут ждать, когда парк расширится автоматически от нагрузки
        if (poolProperty.getMin() == 0 && getRealActiveItem() == 0) {
            return add();
        }
        // Если нет возможности говорим - что не создали
        return false;
    }

    @Override
    public void releasePoolItem(T poolItem, Throwable e) {
        if (poolItem == null) {
            return;
        }
        // Если ошибка является критичной для пловца - выбрасываем его из пула
        if (e != null && poolItem.checkFatalException(e)) {
            exceptionQueue.add(poolItem);
            return;
        }
        // Если произошло ручное удаление, и объект не является уже участником пула - завершаем
        if (!itemQueue.contains(poolItem)) {
            return;
        }
        //Объект, который возвращают в пул не может попасть на удаление, его как бы только что использовали! Алё?
        poolItem.markActive();
        addToPark(poolItem);
        tpsComplete.incrementAndGet();
    }

    public T get() {
        // Забираем с конца, что бы под нож улетели первые добавленные
        while (!parkQueue.isEmpty()) {
            T poolItem = parkQueue.pollLast();
            updateParkStatistic();
            if (poolItem == null) {
                continue;
            }
            if (poolItem.isValid()) {
                return poolItem;
            } else {
                exceptionQueue.add(poolItem);
            }
        }
        return null;
    }

    private boolean add() {
        if (isSizePoolAllowsExtend()) {
            T poolItem = removeQueue.poll();
            if (poolItem != null) {
                addToPark(poolItem);
                return true;
            }
            poolItem = createPoolItem();
            if (poolItem != null) {
                //#1
                itemQueue.add(poolItem);
                //#2 блокировка не нужна, так как элемент только что был создан
                addToPark(poolItem);
                return true;
            }
        }
        return false;
    }

    private void addToPark(@NonNull T poolItem) {
        // Если есть потребители, которые ждут ресурс - отдаём ресурс без перевставок в park
        if (forwardResourceWithoutParking(poolItem)) {
            // updateParkStatistic вызывается планомерно в keepAlive
            // Для быстродействия вычисления нехватки ресурсов в пуле дополнительно вызовем тут
            updateParkStatistic();
            return;
        }
        // Вставка poolItem в parkQueue должна быть только в этом месте
        // Я написал блокировку на вставку в паркинг, что бы сделать атомарной операцию contains и addLast
        // Только что бы избежать дублей в паркинге
        // В обычной жизни конечно такое не должно произойти, но защищаемся всё равно
        boolean addable = false;
        lockAddToPark.lock();
        if (!parkQueue.contains(poolItem)) {
            parkQueue.addLast(poolItem);
            updateParkStatistic();
            addable = true;
        } else {
            App.error(new RuntimeException("Этот код не должен был случиться! Проверить логику! " + poolItem.hashCode()));
        }
        lockAddToPark.unlock();
        // После разблокировки только начинаем заниматься работой
        if (addable) {
            onParkUpdate();
        }
    }

    // Это не явное удаление, а всего лишь маркировка, что в принципе объект может быть удалён
    private boolean addToRemove(@NonNull T poolItem) {
        // Добавлена блокировка, что бы избежать дублей в очереди на удаление
        boolean result = false;
        lockAddToRemove.lock();
        if (getRealActiveItem() > poolProperty.getMin()) {
            result = true;
            // Что если объект уже был добавлен в очередь на удаление?
            // Мы должны вернуть result = true по факту удаление состоялось
            if (!removeQueue.contains(poolItem)) {
                removeQueue.add(poolItem);
            }
        }
        lockAddToRemove.unlock();
        return result;
    }

    // Бывают случаи когда что-то прекращает работать само собой и надо просто вырезать из пула ссылки
    // Но ссылки могут быть
    public void remove(@NonNull T poolItem) {
        itemQueue.remove(poolItem);
        parkQueue.remove(poolItem);
        updateParkStatistic();
        removeQueue.remove(poolItem);
    }

    // А бывает когда надо удалить ссылки и закрыть ресурс по причине самого пула, что ресурс не нужен
    public void removeAndClose(@NonNull T poolItem) {
        remove(poolItem);
        closePoolItem(poolItem);
    }

    private void removeInactive() {
        final long curTimeMs = System.currentTimeMillis();
        final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
        // Что бы избежать расползание ссылок будем изымать и если всё "ок" добавлять обратно
        int size = parkQueue.size();
        List<T> returns = new ArrayList<>();
        while (!parkQueue.isEmpty() && size > 0) {
            if (maxCounterRemove.get() == 0) {
                break; // return нельзя, так как надо вернуть активных
            }
            // В начале должен быть отстойник, так как активные возвращаются в конец
            // Новые задачи подбирают ресурсы с конца
            T poolItem = parkQueue.pollFirst();
            if (poolItem != null) {
                if (poolItem.isExpired(curTimeMs)) {
                    updateParkStatistic();
                    if (addToRemove(poolItem)) {
                        maxCounterRemove.decrementAndGet();
                    }
                } else {
                    // Мы не можем воспользоваться стандартной функцией возвращения пловца
                    // Так как он поместиться в конец очереди, а мы его как бы из отстойника только что взяли
                    //addToPark(poolItem);
                    returns.add(poolItem);
                    // Так как мы изымаем пловцов с начала паркинга и мы уже встретили не протухшего
                    // Дальнейшее перебирание считаю не уместным
                    break;
                }
            }
            size--;
        }
        if (!returns.isEmpty()) {
            // вставка будет в начало паркинга по элементано
            // Поэтому, что бы вставить как есть мы реверснём список
            // park = [1,2,3,4,5,6] poolFirst + add -> [1,2,3] -> park = [4,5,6]
            // [1,2,3].reversed() -> [3,2,1]; park.addFirst(E) ->
            //      1. [3,4,5,6]
            //      2. [2,3,4,5,6]
            //      3. [1,2,3,4,5,6]
            returns.forEach(this::addToPark);
        }

    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<DataHeader> result = new ArrayList<>();
        int tpsCompleteFlush = tpsComplete.getAndSet(0);
        // Если за последнюю секунду были возвращения элемнтов значит пул активен
        // Если в пуле есть элементы и они не в паркинге - пул тоже активный
        if (tpsCompleteFlush > 0 || (!itemQueue.isEmpty() && parkQueue.isEmpty())) {
            markActive();
        }
        result.add(new DataHeader()
                .setBody(getCascadeKey(ns))
                .put("tpsComplete", tpsCompleteFlush)
                .put("item", itemQueue.size())
                .put("park", parkQueue.size())
                .put("remove", removeQueue.size())
        );
        return result;
    }

    // Этот метод нельзя вызывать под бизнес задачи, система сама должна это контролировать
    public void balance() {
        updateParkStatistic();
        try {
            // Если паркинг был пуст уже больше секунды начнём увеличивать
            if (getTimeParkIsEmpty() > 1000 && parkQueue.isEmpty()) {
                overclocking(formulaAddCount.apply(1));
            } else if (getRealActiveItem() > poolProperty.getMin()) { //Кол-во больше минимума
                // закрываем с прошлого раза всех из отставки
                // так сделано специально, если на следующей итерации будет overclocking
                // что бы можно было достать кого-то из отставки
                closeRetired();
                // Готовим новых кандидатов на отставку
                removeInactive();
            }
        } catch (Exception e) {
            App.error(e);
        }
    }

    // Закрываем кандидатов в отставке
    private void closeRetired() {
        while (!removeQueue.isEmpty()) {
            T poolItem = removeQueue.poll();
            if (poolItem != null) {
                removeAndClose(poolItem);
            }
        }
        //Удаление ошибочных
        while (!exceptionQueue.isEmpty()) {
            T poolItem = exceptionQueue.poll();
            if (poolItem != null) {
                removeAndClose(poolItem);
            }
        }
    }

    @Override
    public void runOperation() {
        overclocking(poolProperty.getMin());
        propertyDispatcher.run();
        registerPool.add(this);
    }

    @Override
    public void shutdownOperation() {
        UtilRisc.forEach(null, itemQueue, this::removeAndClose);
        propertyDispatcher.shutdown();
        registerPool.remove(this);
    }

}
