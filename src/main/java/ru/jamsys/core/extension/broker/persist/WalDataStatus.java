package ru.jamsys.core.extension.broker.persist;

import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Потокобезопасный менеджер для отслеживания статусов данных в WAL (Write-Ahead Log).
 * <p>
 * Основные функции:
 * <ul>
 *   <li>Подписка/отписка элементов в группах</li>
 *   <li>Получение непрочитанных элементов (с начала или конца группы)</li>
 *   <li>Контроль интервалов повторной обработки через {@code timeRetryMs}</li>
 * </ul>
 *
 * <p>Все операции потокобезопасны и оптимизированы для многопоточной работы.
 */
public class WalDataStatus {

    /**
     * Задержка (в миллисекундах) перед повторной обработкой элемента.
     * По умолчанию: 60 000 мс (1 минута).
     * <p>
     * Может быть изменено через {@link #setTimeRetryMs(long)}.
     */
    @Setter
    private volatile long timeRetryMs = 60_000;

    /**
     * Основное хранилище данных:
     * <pre>
     *   ID группы → (ID элемента → Время последней обработки)
     * </pre>
     * Где:
     * <ul>
     *   <li>{@code null} - элемент ещё не обрабатывался</li>
     *   <li>Не-null значение - timestamp когда последний раз была выдача элемента</li>
     * </ul>
     */
    private final ConcurrentHashMap<Short, ConcurrentSkipListMap<Long, Long>> map = new ConcurrentHashMap<>();

    /**
     * Блокировки для каждой группы (обеспечивают потокобезопасность операций).
     */
    private final ConcurrentHashMap<Short, AtomicBoolean> groupLock = new ConcurrentHashMap<>();

    /**
     * Регистрирует элемент в указанных группах.
     *
     * @param id       ID элемента
     * @param idGroups Список ID групп для подписки
     * @throws IllegalArgumentException если {@code idGroups} равен null
     * @example // Добавление элемента 123 в группы 1 и 2
     * subscribe(123L, List.of((short)1, (short)2));
     */
    public void subscribe(long id, List<Short> idGroups) {
        long now = System.currentTimeMillis();
        idGroups.forEach(
                idGroup -> map
                        .computeIfAbsent(idGroup, _ -> new ConcurrentSkipListMap<>())
                        .put(id, now - timeRetryMs)
        );
    }

    /**
     * Удаляет элемент из группы.
     *
     * @param id      ID элемента
     * @param idGroup ID группы
     * @example // Удаление элемента 123 из группы 1
     * unsubscribe(123L, (short)1);
     */
    public void unsubscribe(long id, Short idGroup) {
        Map<Long, Long> longLongMap = map.get(idGroup);
        if (longLongMap != null) {
            longLongMap.remove(id);
        }
    }

    /**
     * Возвращает непрочитанные элементы <b>с начала</b> указанной группы.
     * <p>
     * Элемент считается непрочитанным если:
     * <ul>
     *   <li>Он никогда не обрабатывался ({@code timestamp == null}), ИЛИ</li>
     *   <li>Прошло больше {@code timeRetryMs} с момента последней обработки</li>
     * </ul>
     *
     * @param idGroup ID группы
     * @param size    Максимальное количество элементов
     * @return Список ID элементов для обработки (может быть пустым)
     * @example // Получить первые 10 непрочитанных из группы 1
     * List<Long> unprocessed = getFirst((short)1, 10);
     */
    public List<Long> getFirst(Short idGroup, int size) {
        return getFirst(idGroup, size,  System.currentTimeMillis());
    }

    public List<Long> getFirst(Short idGroup, int size, long now) {
        List<Long> result = new ArrayList<>();
        AtomicBoolean lock = groupLock.computeIfAbsent(idGroup, _ -> new AtomicBoolean(false));
        if (lock.compareAndSet(false, true)) {
            try {
                ConcurrentSkipListMap<Long, Long> groupMap = map.get(idGroup);
                if (groupMap == null || groupMap.isEmpty()) {
                    return result;
                }
                Map.Entry<Long, Long> idEntity = groupMap.firstEntry();
                while (size > 0 && idEntity != null) {
                    Long timestamp = idEntity.getValue();
                    Long id = idEntity.getKey();
                    if ((now >= timestamp + timeRetryMs) && groupMap.replace(id, timestamp, now)) {
                        result.add(id);
                        size--;
                    }
                    idEntity = groupMap.higherEntry(idEntity.getKey());
                }
            } finally {
                lock.set(false);
            }
        }
        return result;
    }

    /**
     * Возвращает непрочитанные элементы <b>с конца</b> указанной группы.
     * <p>
     * Аналогично {@link #getFirst(Short, int)}, но обход начинается с конца.
     */

    public List<Long> getLast(Short idGroup, int size) {
        return getLast(idGroup, size,  System.currentTimeMillis());
    }

    public List<Long> getLast(Short idGroup, int size, long now) {
        List<Long> result = new ArrayList<>();
        AtomicBoolean lock = groupLock.computeIfAbsent(idGroup, _ -> new AtomicBoolean(false));
        if (lock.compareAndSet(false, true)) {
            try {
                ConcurrentSkipListMap<Long, Long> groupMap = map.get(idGroup);
                if (groupMap == null || groupMap.isEmpty()) {
                    return result;
                }
                Map.Entry<Long, Long> idEntity = groupMap.lastEntry();
                while (size > 0 && idEntity != null) {
                    Long timestamp = idEntity.getValue();
                    Long id = idEntity.getKey();
                    if ((now >= timestamp + timeRetryMs) && groupMap.replace(id, timestamp, now)) {
                        result.add(id);
                        size--;
                    }
                    idEntity = groupMap.lowerEntry(idEntity.getKey());
                }
            } finally {
                lock.set(false);
            }
        }
        return result;
    }

}
