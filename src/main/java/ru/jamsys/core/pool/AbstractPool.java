package ru.jamsys.core.pool;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerRateLimit;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.rate.limit.item.RateLimitItemInstance;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

// Отработанные пловцы добавляются в парк в конец очереди
// На работу пловцы отправляются из парка с конца очереди
// Таким образом в парке в начале очереди будут тушиться пловцы без работы до их кончины

@ToString(onlyExplicitlyIncluded = true)
public abstract class AbstractPool<RA, RR, PI extends ExpirationMsMutable & Resource<RA, RR>>
        extends ExpirationMsMutableImpl
        implements Pool<RA, RR, PI>, LifeCycleInterface, KeepAlive, ClassName {

    @Getter
    @ToString.Include
    public final String name;

    protected final ConcurrentLinkedDeque<PI> parkQueue = new ConcurrentLinkedDeque<>();

    protected final ConcurrentLinkedQueue<PI> removeQueue = new ConcurrentLinkedQueue<>();

    protected final ConcurrentLinkedQueue<PI> exceptionQueue = new ConcurrentLinkedQueue<>();

    // Общая очередь, где находятся все объекты
    protected final Set<PI> itemQueue = Util.getConcurrentHashSet();

    protected final AtomicBoolean isRun = new AtomicBoolean(false);

    @Getter
    protected final RateLimit rateLimit;

    @Getter
    protected final RateLimit rateLimitPoolItem;

    private final AtomicBoolean restartOperation = new AtomicBoolean(false);

    @Setter
    private Function<Integer, Integer> formulaAddCount = (need) -> need;

    @Setter
    private Function<Integer, Integer> formulaRemoveCount = (need) -> need;

    private long timeWhenParkIsEmpty = -1;

    // Используется в виртуальных пулах потоков в RealThreadComponent, что бы не держать пулы для задач, которые уже не
    // исполняются по каким либо причинам
    private final AtomicInteger tpsComplete = new AtomicInteger(0);

    private final AtomicBoolean dynamicPollSize = new AtomicBoolean(false);

    private final RateLimitItem rliPoolSizeMax;
    private final RateLimitItem rliPoolSizeMin;

    private final Lock lockAddToPark = new ReentrantLock();

    private final Lock lockAddToRemove = new ReentrantLock();

    public AbstractPool(String name, Class<PI> cls) {
        this.name = getClassName(name);

        ManagerRateLimit managerRateLimit = App.get(ManagerRateLimit.class);
        rateLimit = managerRateLimit.get(name)
                .init(App.context, RateLimitName.POOL_SIZE_MAX.getName(), RateLimitItemInstance.MAX)
                .init(App.context, RateLimitName.POOL_SIZE_MIN.getName(), RateLimitItemInstance.MIN);
        rliPoolSizeMax = rateLimit.get(RateLimitName.POOL_SIZE_MAX.getName());
        rliPoolSizeMin = rateLimit.get(RateLimitName.POOL_SIZE_MIN.getName());

        rateLimitPoolItem = managerRateLimit.get(ClassNameImpl.getClassNameStatic(cls, name));
    }

    public void setDynamicPollSize(boolean dynamic) {
        dynamicPollSize.set(dynamic);
    }

    public boolean allInPark() {
        return parkQueue.size() == itemQueue.size();
    }

    // Сколько времени паркинг был пуст
    private long getTimeParkIsEmpty() {
        if (timeWhenParkIsEmpty == -1) {
            return 0;
        } else {
            return System.currentTimeMillis() - timeWhenParkIsEmpty;
        }
    }

    protected void updateParkStatistic() {
        if (parkQueue.isEmpty()) {
            if (timeWhenParkIsEmpty == -1) {
                timeWhenParkIsEmpty = System.currentTimeMillis();
            }
        } else {
            timeWhenParkIsEmpty = -1;
        }
    }

    public boolean isEmpty() {
        return itemQueue.isEmpty();
    }

    public void setPoolSizeMax(int want) {
        if (!dynamicPollSize.get()) {
            return;
        }
        // Если хотят меньше минимума - очень резко опускаем максимум до минимума
        if (want < rliPoolSizeMin.get()) {
            setRliPoolSizeMax(rliPoolSizeMin.get());
            return;
        }
        // Если желаемое значение элементов в пуле больше минимума, так как return не сработал
        if (want > rliPoolSizeMax.get()) { //Медленно поднимаем
            setRliPoolSizeMax(rliPoolSizeMax.get() + 1);
        } else { //Но очень быстро опускаем
            setRliPoolSizeMax(want);
        }
    }

    private void setRliPoolSizeMax(int limit) {
        rliPoolSizeMax.set("max", limit);
    }

    // Бассейн может поместить новые объекты для плаванья
    private boolean isSizePoolAllowsExtend() {
        return isRun.get() && rliPoolSizeMax.check(itemQueue.size());
    }

    // Увеличиваем кол-во объектов для плаванья
    private void overclocking(long count) {
        for (int i = 0; i < count; i++) {
            if (!add()) {
                break;
            }
        }
    }

    public boolean addIfPoolEmpty() {
        if (rliPoolSizeMin.get() == 0 && itemQueue.isEmpty()) {
            return add();
        }
        return false;
    }

    private boolean add() {
        if (isSizePoolAllowsExtend()) {
            PI poolItem = removeQueue.poll();
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

    @Override
    public void complete(PI poolItem, Throwable e) {
        if (poolItem == null) {
            return;
        }
        // Если произошло ручное удаление, и объект не является уже участником пула - завершаем
        if (!itemQueue.contains(poolItem)) {
            return;
        }
        // Если ошибка является критичной для пловца - выбрасываем его из пула
        if (e != null && checkFatalException(e)) {
            exceptionQueue.add(poolItem);
            return;
        }
        //Объект, который возвращают в пул не может попасть на удаление, его как бы только что использовали! Алё?
        poolItem.active();
        addToPark(poolItem);
        tpsComplete.incrementAndGet();
    }

    private void addToPark(@NonNull PI poolItem) {
        // Вставка poolItem в parkQueue должна быть только в этом месте (+ системный addToParkReturnBeforeCheckInactivity)
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
        // После разблокировки только начинаем заниматься грязной работой
        if (addable) {
            onParkUpdate();
        }
    }

    // Это не явное удаление, а всего лишь маркировка, что в принципе объект может быть удалён
    private boolean addToRemove(@NonNull PI poolItem) {
        // Добавлена блокировка, что бы избежать дублей в очереди на удаление
        boolean result = false;
        lockAddToRemove.lock();
        if (rliPoolSizeMin.check(itemQueue.size())) {
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
    public void remove(@NonNull PI poolItem) {
        itemQueue.remove(poolItem);
        parkQueue.remove(poolItem);
        updateParkStatistic();
        removeQueue.remove(poolItem);
    }

    // А бывает когда надо удалить ссылки и закрыть ресурс по причине самого пула, что ресурс не нужен
    public void removeAndClose(@NonNull PI poolItem) {
        remove(poolItem);
        closePoolItem(poolItem);
    }

    // Не конкурентный вызов
    private void removeInactive() {
        if (isRun.get()) {
            final long curTimeMs = System.currentTimeMillis();
            final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
            // ЧТо бы избежать расползание ссылок будем изымать и если всё "ок" добавлять обратно
            int size = parkQueue.size();
            List<PI> returns = new ArrayList<>();
            while (!parkQueue.isEmpty() && size > 0) {
                if (maxCounterRemove.get() == 0) {
                    break; // return нельзя, так как надо вернуть активных
                }
                // В начале должен быть отстойник, так как активные возвращаются в конец
                // Новые задачи подбирают ресурсы с конца
                PI poolItem = parkQueue.pollFirst();
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
    }

    @Override
    public void run() {
        if (restartOperation.compareAndSet(false, true)) {
            isRun.set(true);
            overclocking(rliPoolSizeMin.get());
            restartOperation.set(false);
        }
    }

    @Override
    public void shutdown() {
        if (restartOperation.compareAndSet(false, true)) {
            isRun.set(false);
            UtilRisc.forEach(
                    null,
                    itemQueue,
                    this::removeAndClose
            );
            restartOperation.set(false);
        }
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        int tpsCompleteFlush = tpsComplete.getAndSet(0);
        if (tpsCompleteFlush > 0) {
            active();
        }
        result.add(new Statistic(parentTags, parentFields)
                .addField("tpsComplete", tpsCompleteFlush)
                .addField("min", rliPoolSizeMin.get())
                .addField("max", rliPoolSizeMax.get())
                .addField("item", itemQueue.size())
                .addField("park", parkQueue.size())
                .addField("remove", removeQueue.size())
        );
        return result;
    }

    // Этот метод нельзя вызывать под бизнес задачи, система сама должна это контролировать
    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        if (isRun.get()) {
            try {
                // Если паркинг был пуст уже больше секунды начнём увеличивать
                if (getTimeParkIsEmpty() > 1000 && parkQueue.isEmpty()) {
                    overclocking(formulaAddCount.apply(1));
                } else if (itemQueue.size() > rliPoolSizeMin.get()) { //Кол-во больше минимума
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
    }

    // Закрываем кандидатов в отставке
    private void closeRetired() {
        while (!removeQueue.isEmpty()) {
            PI poolItem = removeQueue.poll();
            if (poolItem != null) {
                removeAndClose(poolItem);
            }
        }
        //Удаление ошибочных
        while (!exceptionQueue.isEmpty()) {
            PI poolItem = exceptionQueue.poll();
            if (poolItem != null) {
                removeAndClose(poolItem);
            }
        }
    }

}
