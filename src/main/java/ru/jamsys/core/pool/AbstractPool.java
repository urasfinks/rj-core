package ru.jamsys.core.pool;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.manager.RateLimitManager;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.RateLimitName;
import ru.jamsys.core.rate.limit.item.RateLimitItem;
import ru.jamsys.core.rate.limit.item.RateLimitItemInstance;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.ExpirationMs;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;
import ru.jamsys.core.util.Util;
import ru.jamsys.core.util.UtilRisc;

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

@SuppressWarnings({"unused", "UnusedReturnValue"})
@ToString(onlyExplicitlyIncluded = true)
public abstract class AbstractPool<T extends Pollable & ExpirationMs>
        extends ExpirationMsMutableImpl implements Pool<T>, RunnableInterface, KeepAlive, ClassName {

    @Getter
    @ToString.Include
    public final String name;

    protected final ConcurrentLinkedDeque<T> parkQueue = new ConcurrentLinkedDeque<>();

    protected final ConcurrentLinkedQueue<T> removeQueue = new ConcurrentLinkedQueue<>();

    protected final ConcurrentLinkedQueue<T> exceptionQueue = new ConcurrentLinkedQueue<>();

    // Общая очередь, где находятся все объекты
    protected final Set<T> itemQueue = Util.getConcurrentHashSet();

    protected final AtomicBoolean isRun = new AtomicBoolean(false);

    @Getter
    protected final RateLimit rateLimit;

    @Getter
    protected final RateLimit rateLimitPoolItem;

    private final int min; //Минимальное кол-во ресурсов

    private final AtomicBoolean restartOperation = new AtomicBoolean(false);

    @Setter
    private Function<Integer, Integer> formulaAddCount = (need) -> need;

    @Setter
    private Function<Integer, Integer> formulaRemoveCount = (need) -> need;

    private long timeWhenParkIsEmpty = -1;

    private final AtomicInteger tpsDequeue = new AtomicInteger(0);

    private final AtomicBoolean dynamicPollSize = new AtomicBoolean(false);

    private final RateLimitItem rliPoolSize;

    private final Lock lockAddToPark = new ReentrantLock();

    private final Lock lockAddToRemove = new ReentrantLock();

    public AbstractPool(String name, int min, Class<T> cls) {
        this.name = name;
        this.min = min;
        RateLimitManager rateLimitManager = App.context.getBean(RateLimitManager.class);
        rateLimit = rateLimitManager.get(getClassName(name))
                .init(RateLimitName.POOL_SIZE.getName(), RateLimitItemInstance.MAX);
        rliPoolSize = rateLimit.get(RateLimitName.POOL_SIZE.getName());

        rateLimitPoolItem = rateLimitManager.get(ClassNameImpl.getClassNameStatic(cls, name));
    }

    public void subscribeOnComplete(){

    }

    public void setDynamicPollSize(boolean dynamic) {
        dynamicPollSize.set(dynamic);
    }

    public boolean allInPark() {
        return parkQueue.size() == itemQueue.size();
    }

    private long getTimeWhenParkIsEmpty() {
        if (timeWhenParkIsEmpty == -1) {
            return 0;
        } else {
            return System.currentTimeMillis() - timeWhenParkIsEmpty;
        }
    }

    private void updateStatistic() {
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

    public void setMaxSlowRiseAndFastFall(int max) {
        if (dynamicPollSize.get()) {
            if (max >= min) {
                if (max > rliPoolSize.getMax()) { //Медленно поднимаем
                    rliPoolSize.incrementMax();
                } else { //Но очень быстро опускаем
                    rliPoolSize.setMax(max);
                }
            } else {
                Util.logConsole("Pool [" + getName() + "] sorry max = " + max + " < Pool.min = " + min, true);
            }
        }
    }

    private boolean isSizePoolAllowsExtend() {
        return isRun.get() && rliPoolSize.check(itemQueue.size());
    }

    private boolean add() {
        if (isSizePoolAllowsExtend()) {
            if (!removeQueue.isEmpty()) {
                T poolItem = removeQueue.poll();
                if (poolItem != null) {
                    addToPark(poolItem);
                    return true;
                }
            }
            final T poolItem = createPoolItem();
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
    public void complete(T poolItem, Exception e) {
        if (poolItem == null) {
            return;
        }
        if (checkExceptionOnComplete(e)) {
            exceptionQueue.add(poolItem);
            return;
        }
        if (!isSizePoolAllowsExtend() && addToRemove(poolItem)) {
            return;
        }
        // Если взять потоки как ресурсы, то после createPoolItem вызывается Thread.start, который после работы сам
        // себя вносит в пул отработанных (parkQueue)
        // Но вообще понятие ресурсов статично и они не живут собственной жизнью
        // поэтому после createPoolItem мы кладём их в parkQueue, что бы их могли взять желающие
        // И для потоков тут получается первичный дубль
        addToPark(poolItem);
    }

    private void addToPark(@NonNull T poolItem) {
        // Вставка poolItem в parkQueue должна быть только в этом месте
        // Я написал блокировку на вставку в паркинг, что бы сделать атомарной операцию contains и addLast
        // Только что бы избежать дублей в паркинге
        lockAddToPark.lock();
        if (!parkQueue.contains(poolItem)) {
            parkQueue.addLast(poolItem);
            updateStatistic();
        }
        lockAddToPark.unlock();
    }

    // Бывают случаи когда что-то прекращает работать само собой и надо просто вырезать из пула ссылки
    public void remove(@NonNull T poolItem) {
        itemQueue.remove(poolItem);
        parkQueue.remove(poolItem);
        updateStatistic();
        removeQueue.remove(poolItem);
    }

    // А бывает когда надо удалить ссылки и закрыть ресурс по причине самого пула, что ресурс не нужен
    public void removeAndClose(@NonNull T poolItem) {
        remove(poolItem);
        closePoolItem(poolItem);
    }

    private void overclocking(int count) {
        for (int i = 0; i < count; i++) {
            if (!add()) {
                break;
            }
        }
    }

    public void addPoolItemIfEmpty() {
        if (min == 0 && itemQueue.isEmpty() && parkQueue.isEmpty()) {
            add();
        }
    }

    // Это не явное удаление, а всего лишь маркировка, что в принципе ресурс может быть удалён
    private boolean addToRemove(@NonNull T poolItem) {
        // Добавлена блокировка, что бы избежать дублей в очереди на удаление
        lockAddToRemove.lock();
        if (itemQueue.size() > min && !removeQueue.contains(poolItem)) {
            removeQueue.add(poolItem);
            return true;
        }
        lockAddToRemove.unlock();
        return false;
    }

    // Не конкурентный вызов
    private void removeLazy() { //Проверка ждунов, что они давно не вызывались
        if (isRun.get()) {
            final long curTimeMs = System.currentTimeMillis();
            final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
            // ЧТо бы избежать расползание ссылок будем изымать и если всё "ок" добавлять обратно
            int size = parkQueue.size();
            while (!parkQueue.isEmpty() && size > 0) {
                if (maxCounterRemove.get() == 0) {
                    return;
                }
                T poolItem = parkQueue.pollFirst();
                if (poolItem != null) {
                    if (poolItem.isExpired(curTimeMs)) {
                        updateStatistic();
                        if (addToRemove(poolItem)) {
                            maxCounterRemove.decrementAndGet();
                        }
                    } else {
                        addToPark(poolItem);
                    }
                }
                size--;
            }
        }
    }

    @Override
    public void run() {
        if (restartOperation.compareAndSet(false, true)) {
            isRun.set(true);
            overclocking(min);
            rateLimit.setActive(true);
            rateLimitPoolItem.setActive(true);
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
            rateLimit.setActive(false);
            rateLimitPoolItem.setActive(false);
            restartOperation.set(false);
        }
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        int tpsDequeueFlush = tpsDequeue.getAndSet(0);
        if (tpsDequeueFlush > 0) {
            active();
        }
        result.add(new Statistic(parentTags, parentFields)
                .addField("tpsDeq", tpsDequeueFlush)
                .addField("min", min).addField("max", rliPoolSize.getMax())
                .addField("item", itemQueue.size())
                .addField("park", parkQueue.size())
                .addField("remove", removeQueue.size())
        );
        return result;
    }

    public String getMomentumStatistic() {
        //sb.append("timePark0: ").append(getTimeWhenParkIsEmpty()).append("; ");
        return "item: " + itemQueue.size() + "; " +
                "park: " + parkQueue.size() + "; " +
                "remove: " + removeQueue.size() + "; " +
                "isRun: " + isRun.get() + "; " +
                "min: " + min + "; " +
                "max: " + rliPoolSize.getMax() + "; ";
    }

    // Этот метод нельзя вызывать под бизнес задачи, система сама должна это контролировать
    // Если ресурса нет - ждите
    // keepAlive работает на статистике от ресурсов. Если у пула min = 0, этот метод не поможет разогнать
    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        if (isRun.get()) {
            try {
                // Было изначально так: если на парковке никого нет - добавляем ресурс
                // Из-за того, что задача сбора статистики вызывается в параллель с keepAlive
                // Получается, что в паркинге нет никого и мы постоянно добавляем туда ресурс
                // Добавленный ресурс ни кем не используется и попадает под нож, постоянно увеличивая счётчик ресурсов
                // Поэтому добавили доп условие, что за промежуток времени не было complete со статусом isFinish = true
                // Переход на tpoParkQueue оказался провальным, так как иногда задачи выполняются реже чем keepAlive
                // Это приводит к тому, что tpoParkQueue может быть 0 без и аналогичная ситуация с паркингом при
                // одновременном выполнении задачи и keepAlive, когда парк пустой и tpoParkQueue = 0 и мы опять
                // идём в увеличении ресурсов.
                // Сейчас буду проигрывать историю, когда буду считать время когда парк опустел до момента, когда в парк
                // вернулись ресурсы
                // Всё стало хорошо, НО иногда в парк могут возвращаться ресурсы, которые вышли по состоянию
                // isOverflowIteration и по ним не надо считать сброс времени, что кол-во в парке стало больше 0 так как
                // это вынужденная мера
                // Вырезана все isFinish, так как целевое решение по контролю бесконечных задач
                // должно решаться в RateLimit
                if (getTimeWhenParkIsEmpty() > 1000 && parkQueue.isEmpty()) {
                    overclocking(formulaAddCount.apply(1));
                } else if (itemQueue.size() > min) { //Кол-во больше минимума
                    removeLazy();
                }
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }
        // Тут происходит непосредственно удаление
        // Не используем UtilRisk, что бы если пойдёт одновременный процесс добавления из удалённых
        // был шанс урвать из очереди хоть что-то для экономии создания новых poolItem
        while (!removeQueue.isEmpty()) {
            T poolItem = removeQueue.poll();
            if (poolItem != null) {
                removeAndClose(poolItem);
            }
        }
        // Удаление ошибочных
        // Чем хорош UtilRisk - если будет постоянное добавление в очередь, мы можем ту встрять на долго
        // А так что было разгребли и ушли
        UtilRisc.forEach(isThreadRun, exceptionQueue, poolItem -> {
            exceptionQueue.remove(poolItem);
            removeAndClose(poolItem);
        });
    }

    @Override
    public T getPoolItem() {
        if (!isRun.get()) {
            return null;
        }
        tpsDequeue.incrementAndGet();
        // Забираем с начала, что бы под нож улетели последние добавленные
        T poolItem = parkQueue.pollFirst();
        if (poolItem != null) {
            updateStatistic();
            poolItem.polled();
        }
        return poolItem;
    }

    @Override
    public T getPoolItem(long timeOutMs, AtomicBoolean isThreadRun) {
        if (!isRun.get()) {
            return null;
        }
        tpsDequeue.incrementAndGet();
        long finishTimeMs = System.currentTimeMillis() + timeOutMs;
        while (isRun.get() && isThreadRun.get() && finishTimeMs > System.currentTimeMillis()) {
            T poolItem = parkQueue.pollFirst();
            if (poolItem != null) {
                updateStatistic();
                poolItem.polled();
                return poolItem;
            }
            Util.sleepMs(100);
        }
        return null;
    }

}
