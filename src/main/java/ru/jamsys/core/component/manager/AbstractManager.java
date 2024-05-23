package ru.jamsys.core.component.manager;

import lombok.Setter;
import ru.jamsys.core.extension.*;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class AbstractManager<MO extends Closable & ExpirationMsMutable & StatisticsFlush>
        implements
        StatisticsCollectorMap<MO>,
        KeepAlive,
        StatisticsFlushComponent,
        ManagerElementBuilder<MO> {

    protected final Map<String, MO> map = new ConcurrentHashMap<>();

    @Setter
    protected boolean cleanableMap = true;

    @Override
    public Map<String, MO> getMapForFlushStatistic() {
        return map;
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        UtilRisc.forEach(
                isThreadRun,
                map,
                (String key, MO element) -> {
                    if (cleanableMap && element.isExpired()) {
                        map.remove(key);
                        element.close();
                    } else if (element instanceof KeepAlive) {
                        ((KeepAlive) element).keepAlive(isThreadRun);
                    }
                }
        );
    }

    // Скрытая реализация, потому что объекты могут выпадать из общей Map так как у них есть срок жизни
    // Они унаследованы от ExpirationMsMutable, если реализация будет открытой, кто-то может прихранить ссылки на
    // текущий брокер по ключу, а он в какой-то момент времени может быть удалён, а ссылка останется
    // мы будем накладывать в некую очередь, которая уже будет не принадлежать менаджеру
    // и обслуживаться тоже не будет [keepAlive, flushAndGetStatistic] так что - плохая эта затея
    protected <T extends CheckClassItem> T getManagerElement(String key, Class<?> classItem) {
        @SuppressWarnings("unchecked")
        T o = (T) map.computeIfAbsent(key, _ -> build(key, classItem));
        if (o != null && o.checkClassItem(classItem)) {
            return o;
        }
        return null;
    }

    public Map<String, MO> getTestMap() {
        return new HashMap<>(map);
    }

}
