package ru.jamsys.core.pool;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    @Getter
    private final String key;

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

    private final Map<T, ManagerConfiguration<T>> configurationReference = new ConcurrentHashMap<>();

    public AbstractPool(String ns, String key) {
        this.ns = ns;
        this.key = key;
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                property,
                getCascadeKey(ns)
        );
    }

    // Этот метод вызывается обязательно в Manager, так как сам пул является AbstractManagerElement и это прописано в
    // контракте использования элементов
    public void setup(Class<T> cls, Consumer<T> onCreatePoolItem) {
        this.cls = cls;
        this.onCreatePoolItem = onCreatePoolItem;
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("class", getClass())
                .append("ns", ns)
                .append("propertyDispatcherNs", propertyDispatcher.getNs())
                .append("classElement", cls)
                .append("items", items)
                ;
    }

    @SuppressWarnings("all")
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

    /**
     * Добавляет минимальное количество элементов в пул, если все ресурсы завершили работу.
     *
     * @return true, если был добавлен новый элемент, иначе false
     */
    public boolean addIdle() {
        // Если в пуле нет никого проверяем возможность создать новый элемент, но только в том случае,
        // если настройки парка могут быть равны 0 и кол-во активных элементов равны 0.
        // Парк типа просто из-за не активности ушёл в ноль, что бы не тратить ресурсы
        // иначе вернём false и просто задачи будут ждать, когда парк расширится автоматически от нагрузки
        if (property.getMin() == 0 && items.isEmpty()) {
            // addIdle выполняется средством потока, в котором исполняется бизнес логика
            // из-за того, что создание нового элемента может выбросить исключение, надо экранировать
            try {
                return addToParkNewItem();
            } catch (Throwable th) {
                App.error(th);
            }
        }
        // Если нет возможности говорим - что не создали
        return false;
    }

    private void remove(T poolItem) {
        ManagerConfiguration<T> tManagerConfiguration = configurationReference.remove(poolItem);
        if (tManagerConfiguration != null) {
            items.remove(tManagerConfiguration);
            // Его не должно быть в парке
            if (parkQueue.remove(tManagerConfiguration)) {
                App.error(new ForwardException(
                        "Этот код не должен был случиться! Проверить логику! ",
                        new HashMapBuilder<>()
                                .append("tManagerConfiguration", tManagerConfiguration)
                                .append("pool", this)
                ));
            }
        }
    }

    private void remove(ManagerConfiguration<T> tManagerConfiguration) {
        if (tManagerConfiguration != null) {
            items.remove(tManagerConfiguration);
            // Его не должно быть в парке
            if (parkQueue.remove(tManagerConfiguration)) {
                App.error(new ForwardException(
                        "Этот код не должен был случиться! Проверить логику! ",
                        new HashMapBuilder<>()
                                .append("tManagerConfiguration", tManagerConfiguration)
                                .append("pool", this)
                ));
            }
        }
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
            remove(poolItem);
            return;
        }
        if (poolItem.isExpiredIgnoringStop()) {
            remove(poolItem);
            return;
        }
        // Если есть потребители, которые ждут ресурс - отдаём ресурс без перевставок в park
        if (forwardResourceWithoutParking(poolItem)) {
            // Так как мы пробрасываем элемент без перевставки, надо ему чекнуть активацию
            poolItem.markActive();
            // Для быстродействия вычисления нехватки ресурсов в пуле дополнительно вызовем тут
            updateStatistic();
            return;
        }

        ManagerConfiguration<T> tManagerConfiguration = configurationReference.get(poolItem);
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
                App.error(new ForwardException(
                        "Этот код не должен был случиться! Проверить логику! ",
                        new HashMapBuilder<>()
                                .append("tManagerConfiguration", tManagerConfiguration)
                                .append("pool", this)
                ));
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
            T poolItemElement = null;
            // Может случиться, что при item.runOperation() может быть выброшено исключение (jdbc connection refused)
            // Надо такой элемент выбрасывать из пула
            try {
                poolItemElement = poolItem.get();
            } catch (Throwable th) {
                App.error(th);
            }
            if (poolItemElement == null) {
                remove(poolItem);
                continue;
            }
            if (!poolItemElement.isValid()) {
                remove(poolItemElement);
                continue;
            }
            configurationReference.putIfAbsent(poolItemElement, poolItem);
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
                    ns,
                    java.util.UUID.randomUUID().toString(),
                    cls,
                    this.onCreatePoolItem
            );
            items.add(poolItem);
            parkQueue.addLast(poolItem);
            updateStatistic();
            // Получилась такая история: создался конфигуратор jdbc но его старт только при poolItem.get()
            // но сейчас он тут ещё не запущен и мы его уже добавили в items и parkQueue. Далее в onParkUpdate он
            // поднимается и при старте кидает исключение "Connection refused"
            onParkUpdate();
            return true;
        }
        return false;
    }

    // Проверка, что в парке есть элемент, это необходимо для обработки ложных срабатываний (spurious wakeups)
    // при использовании LockSupport.park(), хотя они происходят реже, чем при использовании Object.wait()
    public boolean inPark(@NonNull T poolItem) {
        ManagerConfiguration<T> tManagerConfiguration = configurationReference.get(poolItem);
        return parkQueue.contains(tManagerConfiguration);
    }

    private void removeInactive() {
        final AtomicInteger maxCounterRemove = new AtomicInteger(formulaRemoveCount.apply(1));
        UtilRisc.forEach(null, items, item -> {
            if (maxCounterRemove.get() == 0) {
                return false;
            }
            // Если Manager уже слил элемент + он находит у нас в парке и его получилось удалить
            if (!item.isAlive() && parkQueue.remove(item)) {
                maxCounterRemove.decrementAndGet();
                items.remove(item);
                // Если в Manager нет реального элемента, то получать у ManagerConfiguration.get() уже нет смысла
                // так как прийдёт ново-созданный элемент, которого мы в configurationReference не найдём, поэтому
                // просто переберём все значения карты и найдём нужную нам конфигурацию. Да сложность высокая
                UtilRisc.forEach(null, configurationReference, (t, tManagerConfiguration) -> {
                    if (tManagerConfiguration.equals(item)) {
                        configurationReference.remove(t);
                    }
                });
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
        // Пул активный до тех пор, пока в нём есть элементы, если балансировщик всех удалит и новых создаваться не
        // будет, тогда пол менеджером пойдёт под нож
        result.add(new StatisticDataHeader(getClass(), ns)
                .addHeader("acquire", tpsAcquire.getAndSet(0))
                .addHeader("release", tpsRelease.getAndSet(0))
                .addHeader("item", items.size())
                .addHeader("park", parkQueue.size())
        );
        if (!items.isEmpty()) {
            markActive();
        }
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
