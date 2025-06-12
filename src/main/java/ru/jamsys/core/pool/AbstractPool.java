package ru.jamsys.core.pool;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

// Пул, в нём крутятся обёртки ManagerConfiguration<T>. Жизненным циклом T заведует Manager. Если наступает простой,
// когда в парке есть элементы и ManagerConfiguration<T> уже не имеет ссылки на T в Manager (это означает что Manager
// уже и ManagerConfiguration<T> выбросил) - выбрасываем из пула элемент

public abstract class AbstractPool<T extends AbstractExpirationResource>
        extends AbstractManagerElement
        implements Pool<T> {

    @Getter
    protected final String ns;

    // Общая очередь, где находятся все объекты
    protected final Set<ManagerConfiguration<T>> items = Util.getConcurrentHashSet();

    private final ConcurrentLinkedDeque<ManagerConfiguration<T>> parkQueue = new ConcurrentLinkedDeque<>();

    @Setter
    private Function<Integer, Integer> formulaAddCount = (need) -> need; // Формула добавления кол-ва новых элементов

    @Setter
    private Function<Integer, Integer> formulaRemoveCount = (need) -> need; // Формула удаления

    private final EventTiming eventTimingParkIsEmpty = new EventTiming(); // Время фиксации, что парк был пустой

    private final AtomicInteger tpsRelease = new AtomicInteger(0);

    private final AtomicInteger tpsAcquire = new AtomicInteger(0);

    private final Lock lockAddToPark = new ReentrantLock();

    @Getter
    private final PoolRepositoryProperty property = new PoolRepositoryProperty();

    @Getter
    private final PropertyDispatcher<Integer> propertyDispatcher;

    private Consumer<T> onCreatePoolItem;

    private Class<T> cls;

    public AbstractPool(String ns) {
        this.ns = ns;
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                property,
                getCascadeKey(ns)
        );
    }

    // Этот метод вызывается обязательно в Manager, так как сам пул является AbstractManagerElement и это прописано в
    // контракте использования элементов
    public void setup(Class<T> cls, Consumer<T> onCreate) {
        this.cls = cls;
        this.onCreatePoolItem = onCreate;
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("class", getClass())
                .append("ns", ns)
                .append("classElement", cls)
                .append("items", items)
                ;
    }

    public boolean isParkQueueEmpty() {
        return parkQueue.isEmpty();
    }

    // Сколько времени паркинг был пуст
    public long getTimeParkIsEmpty() {
        return eventTimingParkIsEmpty.eventTimePassed();
    }

    protected void updateStatistic() {
        if (parkQueue.isEmpty()) {
            eventTimingParkIsEmpty.event();
        } else {
            eventTimingParkIsEmpty.reset();
        }
    }

    // Проверка, что пул может поместить новые объекты
    private boolean isSizeExtend() {
        if (!isRun()) {
            App.error(new RuntimeException("Пул " + ns + " не может поместить в себя ничего, так как он выключен"));
        }
        return isRun() && items.size() < property.getMax();
    }

    // Добавить минимально кол-во элементов, если в пуле все поумирали
    public boolean idleIfEmpty() {
        if (!parkQueue.isEmpty()) { // Если в парке есть ресурсы, сразу говорим что всё ок
            return true;
        }
        // Если всё-таки в парке нет никого проверяем возможность создать новый элемент, но только в том случае,
        // если настройки парка могут быть равны 0 и кол-во активных элементов равны 0.
        // Парк типа просто из-за неактивности ушёл в ноль, что бы не тратить ресурсы
        // иначе вернём false и просто задачи будут ждать, когда парк расширится автоматически от нагрузки
        if (property.getMin() == 0 && items.isEmpty()) {
            return addToParkNewItem();
        }
        // Если нет возможности говорим - что не создали
        return false;
    }

    // В пуле не должно быть больше 1000 элементов, поэтому пробежка по всем элементам не критична
    private ManagerConfiguration<T> find(T threadExecutePromiseTask) {
        for (ManagerConfiguration<T> x : items) {
            if (x.equalsElement(threadExecutePromiseTask)) {
                return x;
            }
        }
        return null;
    }

    // Вернуть в пул элемент
    @Override
    public void release(T poolItem, Throwable e) {
        tpsRelease.incrementAndGet();
        if (poolItem == null) {
            return;
        }
        // Если ошибка является критичной для пловца - выбрасываем его из пула
        if (e != null && poolItem.checkFatalException(e)) {
            return;
        }
        if (poolItem.isExpiredWithoutStop()) {
            return;
        }
        // Если есть потребители, которые ждут ресурс - отдаём ресурс без перевставок в park
        if (forwardResourceWithoutParking(poolItem)) {
            // Для быстродействия вычисления нехватки ресурсов в пуле дополнительно вызовем тут
            updateStatistic();
            return;
        }
        ManagerConfiguration<T> tManagerConfiguration = find(poolItem);
        if (tManagerConfiguration == null) {
            return;
        }
        // Вставка poolItem в parkQueue должна быть только в этом месте
        // Я написал блокировку на вставку в паркинг, что бы сделать атомарной операцию contains и addLast
        // Только что бы избежать дублей в паркинге, в обычной жизни конечно такое не должно произойти
        boolean addable = false;
        try {
            lockAddToPark.lock();
            if (!parkQueue.contains(tManagerConfiguration)) {
                parkQueue.addLast(tManagerConfiguration);
                updateStatistic();
                addable = true;
            } else {
                App.error(new RuntimeException("Этот код не должен был случиться! Проверить логику! " + tManagerConfiguration.hashCode()));
            }
        } finally {
            lockAddToPark.unlock();
        }
        // После разблокировки только начинаем заниматься работой
        if (addable) {
            onParkUpdate();
        }
    }

    // Приобрести элемент пула. Логика получения элемента из пула не должна быть связана с расширением пула. Расширение
    // пула происходит отдельным потоком вызывающим balance(), потому что создание нового элемента может быть
    // ресурсоёмкой операцией, например открытие соединения до внешнего ресурса
    public T acquire() {
        tpsAcquire.incrementAndGet();
        // Забираем с конца, что бы под нож улетели первые добавленные
        while (!parkQueue.isEmpty()) {
            ManagerConfiguration<T> poolItem = parkQueue.pollLast();
            updateStatistic();
            if (poolItem == null) {
                continue;
            }
            T poolItemElement = poolItem.get();
            if (!poolItemElement.isValid()) {
                continue;
            }
            return poolItemElement;
        }
        return null;
    }

    // Увеличиваем кол-во объектов для плаванья
    private void overclocking(long count) {
        for (int i = 0; i < count; i++) {
            if (!addToParkNewItem()) {
                break;
            }
        }
    }

    private boolean addToParkNewItem() {
        if (isSizeExtend()) {
            ManagerConfiguration<T> poolItem = ManagerConfiguration.getInstance(
                    cls,
                    java.util.UUID.randomUUID().toString(),
                    ns,
                    onCreatePoolItem
            );
            items.add(poolItem);
            parkQueue.addLast(poolItem);
            updateStatistic();
            return true;
        }
        return false;
    }

    // Проверка, что в парке есть элемент, это необходимо для обработки ложных срабатываний (spurious wakeups)
    // при использовании LockSupport.park(), хотя они происходят реже, чем при использовании Object.wait()
    public boolean inPark(@NonNull T poolItem) {
        ManagerConfiguration<T> tManagerConfiguration = find(poolItem);
        return parkQueue.contains(tManagerConfiguration);
    }

    private void removeInactive() {
        final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
        UtilRisc.forEach(null, items, item -> {
            if (maxCounterRemove.get() == 0) {
                return false;  // return нельзя, так как надо вернуть активных
            }
            if (!item.isAlive() && parkQueue.remove(item)) {
                maxCounterRemove.decrementAndGet();
                items.remove(item);
            }
            return true;
        });
    }

    // Этот метод нельзя вызывать под бизнес задачи, система сама должна это контролировать
    public void balance() {
        updateStatistic();
        try {
            // Если паркинг был пуст уже больше секунды начнём увеличивать
            if (getTimeParkIsEmpty() > 1000 && parkQueue.isEmpty()) {
                overclocking(formulaAddCount.apply(1));
            } else if (items.size() > property.getMin()) { //Кол-во больше минимума
                removeInactive();
            }
        } catch (Exception e) {
            App.error(e);
        }
    }

    @Override
    public List<StatisticDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<StatisticDataHeader> result = new ArrayList<>();
        int tpsReleaseFlush = tpsRelease.getAndSet(0);
        int tpsAcquireFlush = tpsAcquire.getAndSet(0);
        // Если за последнюю секунду были возвращения элементов значит пул активен
        // Если в пуле есть элементы и они не в паркинге - пул тоже активный
        if (tpsReleaseFlush > 0 || tpsAcquireFlush > 0) {
            markActive();
        }
        result.add(new StatisticDataHeader(getClass(), ns)
                .addHeader("acquire", tpsAcquireFlush)
                .addHeader("release", tpsReleaseFlush)
                .addHeader("size", items.size())
                .addHeader("park", parkQueue.size())
        );
        return result;
    }

    @Override
    public void runOperation() {
        if (cls == null) {
            throw new RuntimeException("cls is null");
        }
        overclocking(property.getMin());
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        // Все items будут закрыты Manager без нашего участия
        propertyDispatcher.shutdown();
    }

}
